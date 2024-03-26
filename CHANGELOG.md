# Changelog

## [Unreleased] 

### Added
- Setting up and updating validator pool commission rates
- Support of WalletConnect CCD transfer requests
- Support for WalletConnect verifiable presentation requests (for identity proofs)

### Removed
- Revealing identity attributes when creating an account
- Display of account's revealed identity attributes

### Fixed

- An issue where signing a text message through WalletConnect did not work
- An issue where a dApp could request to get a transaction signed by a different account than the one chosen for the WalletConnect session
- Crashing when received unexpected error from an identity provider
- Exiting the wallet after accepting an identity verification error
- Incorrect environment name in a private key export file for Mainnet
- Improper handling of rejected identity verification when setting up a new wallet
- Showing "Address copied" when copying a transaction hash to the clipboard in the scheduled transfer view
- An issue where the identity name was off-center when the edit name icon was visible
- An issue where exporting transaction logs for an account without any transactions would be stuck at 0%
- "Invalid WalletConnect request" message repeatedly shown if received a request with unsupported transaction type
- Exported private key for file-based initial account being incompatible with concordium-client

### Changed
- Suggest running a recovery when facing account or identity creation errors
- Baker/baking renamed to Validator/validating
- WalletConnect session proposals are now rejected if the namespace or methods are not supported, or if the wallet contains no accounts.
- WalletConnect transaction signing request now shows the receiver
(either smart contract or an account) and amount of CCD to send (not including CIS-2 tokens)
- Transfers tab renamed to Activity on the account details screen

[Unreleased]: https://github.com/Concordium/cryptox-android/compare/0.6.1-qa.5...HEAD
