# BitcoinTicker

Comprises of a Main Activity, foreground service, and WorkManager for background processing.

Function is as follows:

* Main Activity displays network status and current bitcoin rate (refreshes every 30s) by communicating with a ForegroundService which does the network calls and processing, and handles connection disconnects / reconnects via a BroadcastReceiver.
* Option to "start" a background service which will take you through a Permissions flow and eventually do nothing, but mark a service to start later, when you background the app.
* You can edit the amount / fluctuation to be notified for, but it defaults to $.05c.
* Once backgrounded, if enabled, the ForegroundService will terminate, causing the Worker (service) to be scheduled 30 seconds later and send you a notification if the change exceeds the amount specified. After this, it goes to a 15 minute update interval, but you can just return to the activity and then go back out again to trigger another update in 30s.
* Tapping on the notification will take you back to the Main Activity.
