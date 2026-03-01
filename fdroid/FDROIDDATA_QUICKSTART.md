# fdroiddata quickstart

Use this when preparing your `fdroiddata` merge request.

## 1) Clone your fork of fdroiddata

```bash
git clone git@gitlab.com:<your-gitlab-user>/fdroiddata.git
cd fdroiddata
git checkout -b add-droidbert
```

## 2) Copy metadata from this repository

From your Droidbert repo root:

```bash
cp fdroid/com.droidbert.yml /path/to/fdroiddata/metadata/com.droidbert.yml
mkdir -p /path/to/fdroiddata/metadata/com.droidbert/en-US
cp fdroid/com.droidbert/en-US/summary.txt /path/to/fdroiddata/metadata/com.droidbert/en-US/summary.txt
```

Or use the helper script:

```bash
./fdroid/copy-to-fdroiddata.sh /path/to/fdroiddata
```

## 3) Lint locally (optional but recommended)

```bash
cd /path/to/fdroiddata
fdroid lint metadata/com.droidbert.yml
```

## 4) Commit with a minimal message

```bash
git add metadata/com.droidbert.yml metadata/com.droidbert/en-US/summary.txt
git commit -m "Add Droidbert (com.droidbert)"
git push origin add-droidbert
```

## 5) Open merge request on GitLab

Target upstream project: `https://gitlab.com/fdroid/fdroiddata`

Suggested MR title:

```text
New app: Droidbert (com.droidbert)
```

Suggested short MR body:

```markdown
Adds metadata for Droidbert (`com.droidbert`).

- Source: https://github.com/marcel-st/droidbert
- Current version: 0.2.12 (versionCode 14)
- Build: Gradle (`gradle: yes`)
```
