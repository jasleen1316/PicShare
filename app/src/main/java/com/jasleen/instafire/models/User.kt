package com.jasleen.instafire.models

// every attrivute of user shpuld belong inside of these classes, and when we
// get or upload data we do it thru this format
// firebase will be able to derive data from the json and map and turn it into the model
data class User(var username: String = "", var age: Int = 0){

}