# Mobile Language Server Protcol IDE

A proof of concept mobile application that connects to
my [Language Server Proxy](https://github.com/MozarellaMan/rust-lsp-proxy) that provides the ability
to implement many language server features over a remote connection.

This app also uses my [Language Proxy Tools library](https://github.com/MozarellaMan/Language-Server-Proxy-Tools) to esablish a connection to the Language Server Proxy, and send and receive messages easily.

## Why?

This app shows that it is possible to develop a fully features mobile development environment with the power of the Language Server Protocol providing features over JSON remotely. Not all of the Language Server Protocol has been implemented in this application, but as the proxy provides a direct connection to the Language Server - all of the protocol's features are possible to support.

## Screenshots

<img alt="app running a java file" src="https://github.com/MozarellaMan/Mobile-LSP-Client/blob/master/screenshots/Screenshot_1614028661.png?raw=true" width="24%"/> <img alt="app highlighting a syntax error" src="https://github.com/MozarellaMan/Mobile-LSP-Client/blob/master/screenshots/Screenshot_1614028721.png?raw=true" width="24%"/> <img alt="app running a java file with compilation error" src="https://github.com/MozarellaMan/Mobile-LSP-Client/blob/master/screenshots/Screenshot_1614028777.png?raw=true" width="24%"/> <img alt="app running a java file that asks for input" src="https://github.com/MozarellaMan/Mobile-LSP-Client/blob/master/screenshots/Screenshot_1614029105.png?raw=true" width="24%"/> 

## Features

### Currently Implemented

- File synchronization
- Running code + sending input to code
- Code diagnostics and diagnostic highlighting
- Ability to create new files

### Missing

- [Semantic Token](https://microsoft.github.io/language-server-protocol/specification#textDocument_semanticTokens) highlighting support

## How to try it

The minimum Android API level supported is 21.

*Note you MUST be running a [Language Server Proxy](https://github.com/MozarellaMan/rust-lsp-proxy) for the app to connect to.*

### Apk (recommended)

- Downlaod the latest APK in the [releases]() section and install on your device or emulator with ADB.
- Enter the URL of the language server proxy and connect.

### Building it yourself

This app uses a, currently in alpha, declarative Android UI framework: [Jetpack Compose](https://developer.android.com/jetpack/compose)
This framework was a joy to work with and protoype this app quickly and flexibly. At the moment, you need the **latest preview** version of [Android Studio](https://developer.android.com/studio/preview) to build this app yourself.

Once you have the above installed and ready, it is simply a matter of cloning the repository and letting gradle build the app, then running it in an emulator with Android Studio
