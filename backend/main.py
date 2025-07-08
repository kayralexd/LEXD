import os
import uuid
import glob
import subprocess
import requests
from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

app = FastAPI()
DOWNLOAD_DIR = "downloads"
os.makedirs(DOWNLOAD_DIR, exist_ok=True)
app.mount("/files", StaticFiles(directory=DOWNLOAD_DIR), name="files")

class DownloadRequest(BaseModel):
    url: str
    mode: str  # "video_audio", "video_only", "audio_only"

class DownloadResponse(BaseModel):
    filename: str
    file_url: str

@app.post("/download", response_model=DownloadResponse)
async def download_media(req: DownloadRequest):
    url = req.url.strip()
    mode = req.mode.strip()
    uid = uuid.uuid4().hex

    ext = os.path.splitext(url)[1].lower()
    if ext in (".mp4", ".m4a", ".webm", ".mkv", ".mp3"):
        out_name = f"lexd_{uid}{ext}"
        out_path = os.path.join(DOWNLOAD_DIR, out_name)
        try:
            r = requests.get(url, stream=True)
            r.raise_for_status()
            with open(out_path, "wb") as f:
                for chunk in r.iter_content(64 * 1024):
                    f.write(chunk)
        except Exception as e:
            raise HTTPException(500, f"Direct download failed: {e}")
        return DownloadResponse(filename=out_name, file_url=f"/files/{out_name}")

    out_template = f"lexd_{uid}.%(ext)s"
    if mode == "video_audio":
        fmt = "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best"
        extra_args = ["--merge-output-format", "mp4"]
        expected_ext = ".mp4"
    elif mode == "video_only":
        fmt = "bestvideo[ext=mp4]/bestvideo"
        extra_args = []
        expected_ext = ".mp4"
    elif mode == "audio_only":
        fmt = "bestaudio"
        extra_args = ["-x", "--audio-format", "mp3"]
        expected_ext = ".mp3"
    else:
        raise HTTPException(400, "Invalid mode")

    cmd = [
        "yt-dlp",
        "-f", fmt,
        "-P", DOWNLOAD_DIR,
        "-o", out_template,
    ] + extra_args + [url]

    print(f"[LEXD] Command: {' '.join(cmd)}")
    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    print(proc.stdout)

    pattern = os.path.join(DOWNLOAD_DIR, f"lexd_{uid}*")
    files = glob.glob(pattern)
    files = [f for f in files if f.lower().endswith(expected_ext)]
    if not files:
        files = glob.glob(pattern)
    if not files:
        raise HTTPException(500, "File not found")

    final_path = sorted(files, key=os.path.getmtime)[-1]
    filename = os.path.basename(final_path)
    return DownloadResponse(filename=filename, file_url=f"/files/{filename}")