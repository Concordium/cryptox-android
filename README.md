# <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" alt="Icon" style="vertical-align: bottom; height: 36px;"/>  CryptoX Concordium Wallet

Use CryptoX Concordium Wallet to get started with the open-source, privacy-centric and public Concordium Blockchain. 

![Screenshots](https://github.com/Concordium/cryptox-android/assets/5675681/1e95cd50-bf59-4484-b59a-134ec7952d89)

With CryptoX Wallet, you can: 
- Create digital identities and your initial account via an identity provider (IDP)
- Create additional accounts with your digital identities
- Send and receive CCD, both via regular and shielded transfers
- Check your account balances, both public and private
- Transfer CCD between your balances (from public to private and from private to public)
- Manage CCD baking and delegation
- Check your CCD release schedule (only for buyers)
- Manage your addresses in the address book for fast and easy transactions
- Export and import backups of your accounts, identities, address book, and keys
- Manage CIS-2 tokens
- Connect to Concordium dApps with WalletConnect

## What is Concordium?
[Concordium](https://www.concordium.com/) is a blockchain-based technology project 
that aims to create a decentralized, secure, and scalable platform for business applications. 
It is designed to provide a transparent and compliant blockchain infrastructure with 
built-in identity verification at the protocol level. This makes it suitable for businesses 
and organizations that require a reliable, efficient, and regulatory-compliant blockchain solution.

## What is CryptoX?
CryptoX is a Concordium wallet with an advanced set of features. 
It is based on Concordium reference wallet.

## Download
[<img alt='Get it on Google Play' width=200 src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/>](https://play.google.com/store/apps/details?id=com.pioneeringtechventures.wallet&hl=en)

## Development notes

### Build variants
- Testnet (`tstnet`) – Public Concordium test network, fake funds and identities. Spaceseven stage
- Stagenet (`stagenet`) – Unstable Concordium test network, fake funds and identities.
No Spaceseven instance
- Mainnet (`mainnet`) – Public Concordium network, real funds and identities. Spaceseven production

### Use Firebase App Distribution
1. Create a JSON key for `app-distribution@concordiummobilewallet.iam.gserviceaccount.com`
in the Google Cloud IAM of the `ConcordiumMobileWallet` (or ask for existing key)
2. Place the key to `app/secret/app-distribution-key.json`
3. Set up a quick runnable Gradle action in Android Studio:
   - For Testnet: `app:assembleTestnetRelease app:appDistributionUploadTestnetRelease`
   - For Stagenet: `app:assembleStagenetRelease app:appDistributionUploadStagenetRelease`
4. Maintain `app/app-distribution-release-notes.txt` to give testers useful context

### Sign app bundle for Google Play
1. Acquire an upload keystore from the company credential storage
2. Build the signed bundle manually
