# Release Notes

## 1.14.0
 - Added support new deeplinks protocol
 - Added information about last updated block
 - Fixed cancel button at external transaction activity after app recreated

## 1.13.0
 - Texasnet release

## 1.10.0
 - Now app supports only android starting version 5.0 (api 21)
 - Added check redeeming ability

## 1.9.1
 - Fixed QR transactions decoding

## 1.9.0
 - Added ability to scan external transaction or open with deeplink
 - Adopted to Android 10
 - Reduced app size
 - Fixed auto-exposure in QR-code scanner

## 1.7.2
 - Fixed available balance
 - Fixed updating balance

## 1.7.1
 - Fixed autocomplete BIP order
 - Sorting coins in accounts by name, BIP always on top
 - Switchable balance fixes
 - Separate decimals

## 1.7.0
 - Payload in transactions
 - Fixed outgoing check
 - Switching balance: bip/all coins/all coins in USD
 - Sorting coins in autocomplete by it reserve (descending)
 - Autolock app after idling 30 seconds in background state
 - Ability to send 0 coins
 

## 1.6.0
 - Added PIN-code
 - Added Fingerprint auth
 - Added button to show mnemonic for authenticated user

## 1.5.2
 - Fixed sending to address or public key with leading zero bytes

## 1.5.1
 - Added Payload message to send transaction
 - Added summary delegated stake at coins screen
 - Fixed minor issues

## 1.5.0
 - Fixed non-canonical integer error in signature
 - Fixed "128" transaction
 - Added ability to copy address from transaction
 - Added "testnet" warning banner for netTest* flavor
 - Fixed minor issues

## 1.4.0
 - Added ability to send "delegate" transaction using "send" tab
 - UI improvements
 
## 1.3.2
  - Update for new testnet with mainnet genesis (initial) block
  - UI improvements
  
## 1.3.1
 - Restored auth by username/password, it was a test feature. Created new branch for testnet

## 1.3.0
 - New network api
 - Fixed UI issues
 - Fixed converting issues

## 1.2.8
 - Showing commission for exchanging transactions
 - Updated SDK
 - Fixed some issues
 - Fixed some UI issues

## 1.2.7
 - Updated SDK
 - Written basic UI tests
 - Fixed minor UI issues
 
## 1.2.6 
 - (!) unreleased

## 1.2.5
  - HotFixed error while getting free coins

## 1.2.4
 - Updated blockchain SDK (min/max values to buy/sell)
 - New Centrifuge client

## 1.2.3
 - Fixed sounds
 - Updated SDK
 - Fixed issues


## 1.2.2
- UX sounds
- updated SDK

## 1.2.1
- Fix crash on Meizu devices
- Minor fixes

## 1.2.0
- Updated for new Test Net
- Added "Get FREE coins" button to replace Minter Bip Wallet telegram bot
- Minor fixes

## 1.1.1
- added icons for delegate and unbond transactions
- minor UI fixes
- fix adding fee to approximate convert sum
- removed email from registering activity
- hidden unavailable (for now) buttons from address management
- help link
- cleanup project
- added a few layouts for tablets

## 1.1.0

- added coin autocompletion
- restrict to send/exchange 0 amount
- delegate & unbound transactions details
- minor ui fixes
- fix ability to type digits in coin name text field
- fix decimals format for send/exchange popups
- fix crash for android 9 when trying to change avatar
- fix duplicating text fields for some android versions
- fix recipient name on send screen after dismissing popup