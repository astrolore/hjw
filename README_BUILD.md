# Landroid mod v2.0: build the APK

Phone only (Termux + GitHub), same as before:

1. In Termux, unzip this zip over your existing project folder:
   `unzip -o /storage/emulated/0/Download/landroid_mod_v2.0.zip -d ~/landroid`
2. `cd ~/landroid`
3. `git add -A`
4. `git -c user.name=me -c user.email=me@x.com commit -m "v2.0"`
5. `git push`
6. GitHub > Actions > the new run. When it is green, download the landroid-apk artifact,
   unzip it, and tap the .apk to install it over the old version.

If the run fails, open it and read the "build summary" box at the bottom of the run page.
App id: com.android_cinnamon_bun.egg.landroid - minSdk 26 (Android 8.0).
