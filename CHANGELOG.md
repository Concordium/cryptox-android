# Changelog

## [Unreleased]

### Added

- Notifications for CCD and CIS-2 token transactions
- Concordex exchange and Wert service where CCD can be purchased
- Ability to reveal the wallet private key for those 
  having no ability to reveal the seed phrase
- Ability to use the wallet private key to restore the wallet

### Fixed

- Inability to configure a validator closed for delegation
- Incorrect state of the account tokens page when there are no tokens

## [1.2.0] - 2024-08-27

### Added

- CCD listings – browse exchanges and services where CCD can be purchased
- Optional anonymous analytics powered by Matomo
- Concordium newsfeed
- Support for company identities created with Global FinReg

### Fixed

- Visually increasing the balance after sending CCD instead of decreasing it
- Adding newly created accounts to the address book with a blank name
- Incorrect text colors in dark theme on Xiaomi

### Changed

- The paste button on the recovery phrase input screen is now attached
  to the top of the keyboard hence remains always visible

## [1.1.1] - 2024-06-11

### Changed

- Increased WalletConnect pairing timeout to reduce the number of "Could not connect" errors

## [1.1.0] - 2024-06-07

### Removed

- Shielding – now it is only possible to unshield your balances

### Added

- Ability to unshield your balances from the "More" screen
- Support for WalletConnect binary messages signing
- Ability to paste the phrase from the clipboard when importing a wallet

### Changed

- The "Watch video" link on the welcome screen now opens the account creation tutorial

### Fixed

- A way to get into an empty wallet without confirming the seed phrase

### Changed

- Actualized End User License Terms and Privacy Policy URL

## [1.0.0] - 2024-04-24

### Added

- Setting up and updating validator pool commission rates
- Support for WalletConnect CCD transfer requests
- Ability to see full details of a WalletConnect transaction to sign
- Ability to see full details of a Spaceseven transaction to sign
- Support for WalletConnect verifiable presentation requests (for identity proofs)
- Validation of metadata checksum when adding CIS-2 tokens
- Display of balance/ownership when adding CIS-2 tokens

### Removed

- Revealing identity attributes when creating an account

### Fixed

- An issue where signing a text message through WalletConnect did not work
- An issue where a dApp could request to get a transaction signed by a different account than the
  one chosen for the WalletConnect session
- Crashing when received unexpected error from an identity provider
- Exiting the wallet after accepting an identity verification error
- Incorrect environment name in a private key export file for Mainnet
- Improper handling of rejected identity verification when setting up a new wallet
- Showing "Address copied" when copying a transaction hash to the clipboard in the scheduled
  transfer view
- An issue where the identity name was off-center when the edit name icon was visible
- An issue where exporting transaction logs for an account without any transactions would be stuck
  at 0%
- "Invalid WalletConnect request" message repeatedly shown if received a request with unsupported
  transaction type
- Exported private key for file-based initial account being incompatible with concordium-client
- Inability to search for CIS-2 token by ID on contracts with lots of tokens
- When managing CIS-2 tokens, removing all of them when only unselecting the visible ones
- Composing a letter with a malformed recipient when clicking the support email on the About screen
- Possibility of spamming the app with WalletConnect requests from a malfunctioning dApp

### Changed

- Suggest running a recovery when facing account or identity creation errors
- Baker/baking renamed to Validator/validating
- WalletConnect session proposals are now rejected if the namespace or methods are not supported, or
  if the wallet contains no accounts.
- WalletConnect transaction signing request now shows the receiver
  (either smart contract or an account) and amount of CCD to send (not including CIS-2 tokens)
- Transfers tab renamed to Activity on the account details screen
- Identity data tab on the account details screen is no longer shown for accounts without revealed
  attributes
- CIS-2 tokens with corrupted or missing metadata can no longer be added

[Unreleased]: https://github.com/Concordium/cryptox-android/compare/1.2.0...HEAD

[1.2.0]: https://github.com/Concordium/cryptox-android/compare/1.1.1...1.2.0

[1.1.1]: https://github.com/Concordium/cryptox-android/compare/1.1.0...1.1.1

[1.1.0]: https://github.com/Concordium/cryptox-android/compare/1.0.0...1.1.0

[1.0.0]: https://github.com/Concordium/cryptox-android/compare/0.6.1-qa.5...1.0.0
