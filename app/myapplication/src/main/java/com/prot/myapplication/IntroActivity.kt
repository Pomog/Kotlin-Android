package com.prot.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth(),
        color = Color(0xFFF5F5DC)
    ) {
        Text(text = "Hello Yurii")
        CreateCircle()
    }
}

@Preview
@Composable
fun CreateCircle() {
    Card(
        modifier = Modifier
            .padding(3.dp)
            .size(50.dp)
            .clickable {
                Log.d("Tap", "Circle: Tap")
            },
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "Tap", modifier = Modifier.padding(3.dp))
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