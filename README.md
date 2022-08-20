# Web3 Auction Site
## Overview
This project uses a client-server architecture to simulate a simple auction site. Clients can connect to a server and bid on a few 
items that the server is initialized with. The client can either bid in _dollars_ or _sol_, the native currency on the Solana blockchain. 

The separation of the client and server side is done intentionally to highlight the fact that as long as a server is running, a client can connect 
without any knowledge of server (except IP address). This is done through __socket programming__. 

## Frameworks and Libraries
This project is built in __Java__ orgininally on Eclipse IDE. Technologies used include
- _Gson_ for parsing json files in Java.
- _Jsoup_ api for webscraping price data on Solana.
- _Sqlite_ for recording and storing encrypted client login information.
- _JavaFX_ for creating an interactive GUI on the client-side. 

