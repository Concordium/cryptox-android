# <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" alt="Icon" style="vertical-align: bottom; height: 36px;"/>  CryptoX Concordium Wallet

Use CryptoX Concordium Wallet to get started with the open-source, privacy-centric and public Concordium Blockchain. 

![Screenshots](screens.png)

With CryptoX Wallet, you can:
- Create digital identities and your initial account via an identity provider (IDP)
- Create additional accounts with your digital identities
- Send and receive CCD
- Check your account balances, both public and private
- Transfer CCD between your balances (from public to private and from private to public)
- Manage CCD validation and delegation
- Check your CCD release schedule (only for buyers)
- Manage your addresses in the address book for fast and easy transactions
- Export and import backups of your accounts, identities, address book, and keys
- Import both phrase- and file-based wallets
- Manage CIS-2 tokens
- Browse owned NFTs from [Spaceseven](https://spaceseven.com/marketplace)
- Connect to Concordium dApps with WalletConnect

## What is Concordium?
[Concordium](https://www.concordium.com/) is a blockchain-based technology project 
that aims to create a decentralized, secure, and scalable platform for business applications. 
It is designed to provide a transparent and compliant blockchain infrastructure with 
built-in identity verification at the protocol level. This makes it suitable for businesses 
and organizations that require a reliable, efficient, and regulatory-compliant blockchain solution.

## What is CryptoX?
CryptoX is a Concordium wallet with an advanced set of features. 
It is based on Concordium reference wallet and can be used instead of both Concordium and Concordium Legacy wallets.

## Download
| Mainnet| Testnet|
|:------:|:------:|
|[Google Play](https://play.google.com/store/apps/details?id=com.pioneeringtechventures.wallet&hl=en) &#124; [GitHub](https://github.com/Concordium/cryptox-android/releases/latest)|[Google Play](https://play.google.com/store/apps/details?id=com.pioneeringtechventures.wallet.testnet&hl=en) &#124; [GitHub](https://github.com/Concordium/cryptox-android/releases/latest)|

*APKs attached to GitHub releases have signature different from the packages distributed through GooglePlay, therefore can't be installed over each other without manual uninstall.*

## Development notes

The app requires Java 17 JDK for development.

### Build variants
- Testnet (`tstnet`) – Public Concordium test network, fake funds and identities. Spaceseven stage
- Stagenet (`stagenet`) – Unstable Concordium test network, fake funds and identities.
No Spaceseven instance
- Mainnet (`mainnet`) – Public Concordium network, real funds and identities. Spaceseven production

### Distribution for internal testing
Builds for internal testing tagged as QA (`-qa.X`) are assembled and distributed through Firebase App distribution by the [corresponding pipeline](.github/workflows/firebase_internal_testing.yml).
Update the version in `main`, tag the commit with the version name and push the changes.

[Track the Internal testing pipeline](https://github.com/Concordium/cryptox-android/deployments/Internal%20testing)

APKs can be downloaded from the GitHub workflow artifacts or from the Firebase console.

### Building for release
Builds for releases tagged with semver version (`X.Y.Z`) or release candidate (`-rc.X`) 
are assembled by the [corresponding pipeline](.github/workflows/release_build.yml). 
Update the version in `main`, tag the commit with the version name and push the changes.

[Track the Release pipeline](https://github.com/Concordium/cryptox-android/deployments/Releases)

APKs signed for standalone distribution and bundles signed with the Google Play upload key 
can be downloaded from the GitHub workflow artifacts.
