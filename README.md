# Halone examples

Small, complete apps to start a [Halone](https://github.com/HaloneXR/halone)
app from:

| | |
|---|---|
| [`hello`](hello) | a web app: its window, a call it is denied, a TCP socket |
| [`plugin-sample`](plugin-sample) | a native plugin: a test pattern drawn with a Canvas, and a page of Android views |

They use the published [SDK](https://github.com/HaloneXR/halone-sdk):
`@halone/sdk` and `@halone/cli` from npm, and `dev.halone:plugin-sdk` from
Maven Central.

## Setting up

```sh
git clone git@github.com:HaloneXR/halone-examples.git
cd halone-examples
pnpm install
```

## The web app

```sh
cd hello
pnpm dev          # run it in Halone, with developer mode on
pnpm run pack     # dev.halone.hello-<version>.halone
```

## The plugin

```sh
./gradlew :plugin-sample:installDebug
```

Then turn it on in Halone's Settings → Plugins. The Android build needs
`local.properties` with `sdk.dir`, and JDK 17.

## License

[MIT](LICENSE).
