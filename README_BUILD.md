# Landroid v5.1: build the APK

An unofficial extension of the Landroid game. Phone only (Termux + GitHub):

1. Put `landroid_v5.1.1.zip` in your Download folder.
2. In Termux: `termux-setup-storage`, `pkg install -y git unzip`, then
   `unzip /storage/emulated/0/Download/landroid_v5.1.1.zip -d ~/landroid`
3. `cd ~/landroid`, `git init -b main`, `git add -A`
4. `git -c user.name=me -c user.email=me@x.com commit -m "v5.1"`
5. `git remote add origin https://github.com/astrolore/hjw.git`
6. `git push -u --force origin main`
7. GitHub > Actions > the new run. When it is green, download the landroid-apk artifact,
   unzip it, and tap the .apk to install it over the old version.

If the run fails, open it and read the "build summary" box at the bottom of the run page.
App id: com.android_cinnamon_bun.egg.landroid - minSdk 26 (Android 8.0).
