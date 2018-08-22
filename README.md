# My Crypto Coins 
## Open source cryptocurrencies portfolio app for Android
### Part 3: Architecture patterns MVC, MVP, MVVM with Android Architecture Components - ViewModel, LiveData, Data Binding, Room

![my_crypto_coins_app_architecture](https://user-images.githubusercontent.com/2387056/44463990-2682b800-a622-11e8-9fc9-a174b5fb61be.jpg)
I decided to create an app which would be your portfolio of what cryptocurrencies you hold and let you know how much they are worth converted to fiat money.
The important thing for the user is that this app is going to ensure 100% trust as it will not require any login/registration process and won’t collect user’s data by sending it to the server. I guess nobody would feel comfortable sharing information online about owned money. User’s provided data about cryptocurrency investments will be only stored inside local database that is kept inside Android device. However to know portfolio value converted to the fiat money app is going to use internet to get the latest conversion rates. Money topic for people is so sensitive information, that is why to ensure even more trust I will be developing this app openly by creating blog posts series and making project code available for everyone to see that there is nothing to hide.


## Tutorial
I am creating modern Android sample app using best tools and practices available in year 2018. I am doing this because I want to cover all main hottest topics in Android world and acquire knowledge in them by teaching you. If you follow blog posts series you will learn how the app is being developed from the scratch.

Blog posts series: https://www.baruckis.com/android/my-crypto-coins-app-series

#### This repository branch is created for blog post "Part 3: Architecture patterns MVC, MVP, MVVM with Android Architecture Components - ViewModel, LiveData, Data Binding, Room"
https://www.baruckis.com/android/my-crypto-coins-app-series-part-3

In the third part we started to build a new app with best practices applied. Let’s summarize everything that we manage to make already:

My Crypto Coins app every separate screen has its own ViewModel, which will survive any configuration change and protect user from any data loss. App user interface is reactive type, which means, that it will update immediately and automatically when data changes in the back-end. That is done with the help of LiveData. Our project have less code as we bind to the variables in our code directly using Data Binding. Finally, our app stores user data locally inside device as SQLite database which was created conveniently with Room component. App code is structured by features and all project architecture is MVVM - recommended pattern by Android team.


## License

    Copyright 2018 Andrius Baruckis www.baruckis.com | mycryptocoins.baruckis.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
