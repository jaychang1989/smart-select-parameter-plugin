"""Create an allowlisted source ZIP without local logs, credentials or built plugins."""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import zipfile
from publication_check import ROOT, inspect_source, source_files


def build_archive(root, output):
    result = inspect_source(root)
    if not result["local_preparation_passed"]:
        raise ValueError("Publication source check failed; no archive was created")
    root = Path(root).resolve()
    output = Path(output).resolve()
    if output.exists():
        raise FileExistsError("Refusing to overwrite an existing source archive")
    manifest = {}
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for file in source_files(root):
            relative = file.relative_to(root).as_posix()
            data = file.read_bytes()
            manifest[relative] = hashlib.sha256(data).hexdigest()
            info = zipfile.ZipInfo("smart-select-parameter-plugin/" + relative, (2026, 10, 24, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (0o100755 if relative.endswith(".sh") else 0o100644) << 16
            archive.writestr(info, data)
        archive.writestr("smart-select-parameter-plugin/SOURCE-MANIFEST.json",
                         json.dumps(manifest, indent=2) + "\n")
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    output.with_suffix(output.suffix + ".sha256").write_text(
        digest + "  " + output.name + "\n", encoding="ascii")
    return {"archive": output.name, "sha256": digest, "source_files": len(manifest)}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(build_archive(args.root, args.output), indent=2))
