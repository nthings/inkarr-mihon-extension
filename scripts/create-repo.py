#!/usr/bin/env python3
"""
Inkarr Extension Repository Generator

This script generates index.json and index.min.json files for Mihon/Tachiyomi
extension repositories by inspecting APK files and extracting their metadata.

Usage:
    python create-repo.py [--apk-dir APK_DIR] [--output-dir OUTPUT_DIR] [--repo-url REPO_URL]

Requirements:
    - aapt2 or aapt in PATH (from Android SDK build-tools)
    - Python 3.8+
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path
from typing import Any, Optional


# Default configuration
DEFAULT_APK_DIR = "src/all/inkarr/build/outputs/apk/release"
DEFAULT_OUTPUT_DIR = "repo"
DEFAULT_REPO_URL = "https://raw.githubusercontent.com/nthings/inkarr-mihon-extension/repo"


def find_aapt() -> Optional[str]:
    """Find aapt2 or aapt in PATH or common Android SDK locations."""
    # Try aapt2 first, then aapt
    for tool in ["aapt2", "aapt"]:
        # Check PATH
        result = shutil.which(tool)
        if result:
            return result
        
        # Check common Android SDK locations
        android_home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if android_home:
            build_tools = Path(android_home) / "build-tools"
            if build_tools.exists():
                # Get the latest version
                versions = sorted(build_tools.iterdir(), reverse=True)
                for version in versions:
                    tool_path = version / tool
                    if tool_path.exists():
                        return str(tool_path)
    
    return None


def extract_apk_info_aapt(apk_path: str, aapt_path: str) -> dict[str, Any]:
    """Extract APK metadata using aapt/aapt2."""
    info = {
        "name": "",
        "pkg": "",
        "version": "",
        "code": 0,
        "nsfw": 0,
    }
    
    try:
        # Use aapt dump badging
        if "aapt2" in aapt_path:
            result = subprocess.run(
                [aapt_path, "dump", "badging", apk_path],
                capture_output=True,
                text=True,
                check=True
            )
        else:
            result = subprocess.run(
                [aapt_path, "dump", "badging", apk_path],
                capture_output=True,
                text=True,
                check=True
            )
        
        output = result.stdout
        
        # Extract package name, version code, and version name
        package_match = re.search(
            r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",
            output
        )
        if package_match:
            info["pkg"] = package_match.group(1)
            info["code"] = int(package_match.group(2))
            info["version"] = package_match.group(3)
        
        # Extract application label (name)
        label_match = re.search(r"application-label:'([^']+)'", output)
        if label_match:
            info["name"] = label_match.group(1)
            # Clean up "Tachiyomi: " prefix if present
            if info["name"].startswith("Tachiyomi: "):
                info["name"] = info["name"].replace("Tachiyomi: ", "")
        
        # Extract NSFW flag from meta-data
        nsfw_match = re.search(r"meta-data: name='tachiyomi\.extension\.nsfw' value='(\d+)'", output)
        if nsfw_match:
            info["nsfw"] = int(nsfw_match.group(1))
            
    except subprocess.CalledProcessError as e:
        print(f"Error running aapt: {e.stderr}", file=sys.stderr)
        raise
    
    return info


def extract_apk_info_zipfile(apk_path: str) -> dict[str, Any]:
    """
    Fallback method to extract APK info by reading AndroidManifest.xml.
    Note: This is limited as AndroidManifest.xml in APKs is binary.
    """
    info = {
        "name": "Inkarr",
        "pkg": "eu.kanade.tachiyomi.extension.all.inkarr",
        "version": "1.4.1",
        "code": 1,
        "nsfw": 0,
    }
    
    # Try to read version from build.gradle if available
    build_gradle = Path(apk_path).parent.parent.parent.parent / "build.gradle"
    if build_gradle.exists():
        content = build_gradle.read_text()
        version_match = re.search(r"extVersionCode\s*=\s*(\d+)", content)
        if version_match:
            info["code"] = int(version_match.group(1))
            info["version"] = f"1.4.{info['code']}"
    
    return info


def extract_icon_from_apk(apk_path: str, output_dir: Path, pkg_name: str) -> Optional[str]:
    """Extract the extension icon from the APK."""
    icon_dir = output_dir / "icon"
    icon_dir.mkdir(parents=True, exist_ok=True)
    
    icon_filename = f"{pkg_name}.png"
    icon_path = icon_dir / icon_filename
    
    try:
        with zipfile.ZipFile(apk_path, 'r') as apk:
            # Look for the highest resolution icon
            icon_paths = [
                "res/mipmap-xxxhdpi-v4/ic_launcher.png",
                "res/mipmap-xxxhdpi/ic_launcher.png",
                "res/mipmap-xxhdpi-v4/ic_launcher.png",
                "res/mipmap-xxhdpi/ic_launcher.png",
                "res/mipmap-xhdpi-v4/ic_launcher.png",
                "res/mipmap-xhdpi/ic_launcher.png",
                "res/mipmap-hdpi-v4/ic_launcher.png",
                "res/mipmap-hdpi/ic_launcher.png",
                "res/mipmap-mdpi-v4/ic_launcher.png",
                "res/mipmap-mdpi/ic_launcher.png",
            ]
            
            for icon_zip_path in icon_paths:
                try:
                    with apk.open(icon_zip_path) as src:
                        with open(icon_path, 'wb') as dst:
                            dst.write(src.read())
                    return icon_filename
                except KeyError:
                    continue
                    
    except zipfile.BadZipFile:
        print(f"Warning: Could not read APK as zip: {apk_path}", file=sys.stderr)
    
    return None


def generate_index(
    apk_dir: str,
    output_dir: str,
    repo_url: str,
    keep_existing: bool = True
) -> list[dict[str, Any]]:
    """Generate index.json from APK files."""
    apk_path = Path(apk_dir)
    output_path = Path(output_dir)
    
    # Create output directories
    output_path.mkdir(parents=True, exist_ok=True)
    apk_output_dir = output_path / "apk"
    apk_output_dir.mkdir(parents=True, exist_ok=True)
    
    # Load existing index if keeping existing entries
    existing_index: list[dict[str, Any]] = []
    index_file = output_path / "index.json"
    if keep_existing and index_file.exists():
        try:
            existing_index = json.loads(index_file.read_text())
        except json.JSONDecodeError:
            print("Warning: Could not parse existing index.json", file=sys.stderr)
    
    # Find aapt tool
    aapt_path = find_aapt()
    
    # Process APK files
    extensions: list[dict[str, Any]] = []
    apk_files = list(apk_path.glob("*.apk"))
    
    if not apk_files:
        # Try looking in debug directory as fallback
        debug_path = apk_path.parent / "debug"
        apk_files = list(debug_path.glob("*.apk"))
    
    for apk_file in apk_files:
        print(f"Processing: {apk_file.name}")
        
        # Extract APK info
        if aapt_path:
            info = extract_apk_info_aapt(str(apk_file), aapt_path)
        else:
            print("Warning: aapt not found, using fallback extraction", file=sys.stderr)
            info = extract_apk_info_zipfile(str(apk_file))
        
        # Generate APK filename for repo (format: tachiyomi-{lang}.{name}-v{version}.apk)
        pkg_parts = info['pkg'].split('.')
        lang = pkg_parts[-2] if len(pkg_parts) >= 2 else "all"  # e.g., "all" from eu.kanade.tachiyomi.extension.all.inkarr
        short_name = pkg_parts[-1]  # e.g., "inkarr"
        apk_name = f"tachiyomi-{lang}.{short_name}-v{info['version']}.apk"
        
        # Copy APK to output directory
        dest_apk = apk_output_dir / apk_name
        shutil.copy2(apk_file, dest_apk)
        print(f"  Copied to: {dest_apk}")
        
        # Extract icon
        icon_filename = extract_icon_from_apk(str(apk_file), output_path, info["pkg"])
        
        # Build extension entry
        extension = {
            "name": f"Tachiyomi: {info['name']}",
            "pkg": info["pkg"],
            "apk": apk_name,
            "lang": lang,
            "code": info["code"],
            "version": info["version"],
            "nsfw": info["nsfw"],
        }
        
        if icon_filename:
            extension["hasIcon"] = 1
        
        # Add sources info (required by newer Mihon versions)
        extension["sources"] = [{
            "name": info["name"],
            "lang": lang,
            "id": generate_source_id(info["pkg"]),
            "baseUrl": "",
            "versionId": info["code"]
        }]
        
        extensions.append(extension)
        print(f"  Added: {info['name']} v{info['version']} (code: {info['code']})")
    
    # Merge with existing entries (update existing, add new)
    pkg_map = {ext["pkg"]: ext for ext in existing_index}
    for ext in extensions:
        pkg_map[ext["pkg"]] = ext
    
    final_index = list(pkg_map.values())
    
    # Sort by name
    final_index.sort(key=lambda x: x.get("name", ""))
    
    # Write index.json (pretty)
    index_json = output_path / "index.json"
    index_json.write_text(json.dumps(final_index, indent=2, ensure_ascii=False))
    print(f"\nGenerated: {index_json}")
    
    # Write index.min.json (minified)
    index_min_json = output_path / "index.min.json"
    index_min_json.write_text(json.dumps(final_index, separators=(',', ':'), ensure_ascii=False))
    print(f"Generated: {index_min_json}")
    
    return final_index


def generate_source_id(pkg: str) -> str:
    """Generate a stable source ID from package name (matches Mihon's algorithm)."""
    import hashlib
    key = f"{pkg}/all/1"
    md5 = hashlib.md5(key.encode()).digest()
    # Take first 8 bytes and convert to long
    result = 0
    for i in range(8):
        result = (result << 8) | md5[i]
    return str(result & 0x7FFFFFFFFFFFFFFF)


def create_repo_json(output_dir: str, repo_url: str) -> None:
    """Create repo.json with repository metadata from template or default."""
    output_path = Path(output_dir)
    script_dir = Path(__file__).parent
    template_file = script_dir / "repo-template.json"
    
    # Try to use template file first
    if template_file.exists():
        repo_info = json.loads(template_file.read_text())
        # Update URLs with actual repo URL base
        if "meta" in repo_info:
            base_url = repo_url.rsplit("/repo", 1)[0] if "/repo" in repo_url else repo_url
            repo_info["meta"]["website"] = repo_info["meta"].get("website", "").replace(
                "your-username", base_url.split("/")[-1] if "/" in base_url else "inkarr"
            )
    else:
        # Default template
        repo_info = {
            "meta": {
                "name": "Inkarr Extension Repository",
                "shortName": "Inkarr",
                "description": "Mihon extension for Inkarr - self-hosted manga/comic server",
                "owner": "inkarr",
                "website": "https://github.com/inkarr",
                "support": "https://github.com/inkarr/issues"
            }
        }
    
    repo_json = output_path / "repo.json"
    repo_json.write_text(json.dumps(repo_info, indent=2, ensure_ascii=False))
    print(f"Generated: {repo_json}")


def main():
    parser = argparse.ArgumentParser(
        description="Generate Mihon extension repository index from APK files"
    )
    parser.add_argument(
        "--apk-dir",
        default=DEFAULT_APK_DIR,
        help=f"Directory containing APK files (default: {DEFAULT_APK_DIR})"
    )
    parser.add_argument(
        "--output-dir",
        default=DEFAULT_OUTPUT_DIR,
        help=f"Output directory for repository files (default: {DEFAULT_OUTPUT_DIR})"
    )
    parser.add_argument(
        "--repo-url",
        default=DEFAULT_REPO_URL,
        help="Base URL for the repository"
    )
    parser.add_argument(
        "--no-merge",
        action="store_true",
        help="Don't merge with existing index.json"
    )
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("Inkarr Extension Repository Generator")
    print("=" * 60)
    print(f"APK Directory: {args.apk_dir}")
    print(f"Output Directory: {args.output_dir}")
    print(f"Repository URL: {args.repo_url}")
    print("=" * 60)
    
    try:
        generate_index(
            args.apk_dir,
            args.output_dir,
            args.repo_url,
            keep_existing=not args.no_merge
        )
        create_repo_json(args.output_dir, args.repo_url)
        print("\n✓ Repository generation complete!")
        
    except Exception as e:
        print(f"\n✗ Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
