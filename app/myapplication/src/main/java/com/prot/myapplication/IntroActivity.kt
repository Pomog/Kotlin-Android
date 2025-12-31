package com.prot.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prot.myapplication.ui.theme.MyTestApplicationTheme

class IntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyTestApplicationTheme {

                MyApp()

            }
        }
    }
}

@Composable
fun MyApp() {
    var counter by remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth(),
        color = Color(0xFFF5F5DC)
    ) {
        Text(text = "Hello Yurii")
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$counter", style = TextStyle(
                    color = Color.Red,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(30.dp))
            CreateCircle(counter = counter) {
                counter = it + 1
            }
        }
    }
}

@Composable
fun CreateCircle(counter: Int = 0, updateCounter: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .padding(3.dp)
            .size(50.dp)
            .clickable {
                updateCounter(counter)
            },
        shape = CircleShape, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Tap $counter", modifier = Modifier.padding(3.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyTestApplicationTheme {
        MyApp()
    }
}