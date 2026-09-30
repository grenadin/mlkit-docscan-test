# ML Kit Document Scanner test client

Minimal Android app that calls the ML Kit Document Scanner API through Google Play services, used to test
the `ACTION_SCAN_DOCUMENT` implementation in [microG](https://github.com/microg/GmsCore).

It uses Google's official client library
[`com.google.android.gms:play-services-mlkit-document-scanner`](https://developers.google.com/ml-kit/vision/doc-scanner/android)
exactly as documented by the ML Kit team, following the official sample
[googlesamples/mlkit/android/documentscanner](https://github.com/googlesamples/mlkit/tree/master/android/documentscanner):

- `GmsDocumentScanning.getClient(options).getStartScanIntent(activity)` to start the scanner
- `GmsDocumentScanningResult.fromActivityResultIntent(intent)` to read the result

Options: JPEG + PDF results, gallery import allowed, page limit 5, full scanner mode.

After a scan, the app shows (and logs with tag `DocScanTest`) the result code, each page URI and the PDF URI,
page count and size, so it is visible whether the client library could read the returned files.

## Build

```
./gradlew assembleDebug
```

Requires Android SDK (set `sdk.dir` in `local.properties`) and JDK 17+. Minimum Android version is 6.0 (API 23),
as required by the ML Kit document scanner library.
