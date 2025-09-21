package com.example.splitpay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Preview
@Composable
fun HomeScreen(

) {
    //Whole Screen
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        //topBar
        Row(
            modifier = Modifier
                .shadow(
                    4.dp,
                    shape = RectangleShape
                ),
            //verticalAlignment = Alignment

        ){
           Text(
               text = "HomeScreen"
           )
        }
        //Homescreen Body
        Column(){
            Text(
                text = "Content"
            )
        }

        //BottomNav
        Row(){
            Text(
                text = "BottomNav Icons"
            )
        }
    }
}


