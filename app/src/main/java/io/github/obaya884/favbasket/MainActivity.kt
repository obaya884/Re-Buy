package io.github.obaya884.favbasket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.obaya884.favbasket.ui.theme.FavBasketTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Screen()
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Preview(showBackground = true)
    @Composable
    fun Screen() {
        FavBasketTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background
            ) {
                val scope = rememberCoroutineScope()
                val scaffoldState = rememberBottomSheetScaffoldState()
                BottomSheetScaffold(
                    sheetContent = {
                        Text(
                            text = "Prepared Items",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(colors.primary)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        val preparedItems = viewModel.uiState.collectAsState().value.preparedItems
                        LazyColumn(
                            modifier = Modifier.defaultMinSize(minHeight = 240.dp)
                        ) {
                            items(preparedItems) { item ->
                                InBasketItemCard(item)
                            }
                        }
                    },
                    sheetShape = RoundedCornerShape(12.dp),
                    scaffoldState = scaffoldState,
                    topBar = {
                        TopAppBar(
                            title = { Text("Bottom sheet scaffold") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Localized description"
                                    )
                                }
                            }
                        )
                    },
                    sheetPeekHeight = 128.dp,
                    drawerContent = {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Drawer content")
                            Spacer(Modifier.height(20.dp))
                            Button(onClick = { scope.launch { scaffoldState.drawerState.close() } }) {
                                Text("Click to close drawer")
                            }
                        }
                    }
                ) { innerPadding ->
                    val inBasketItems = viewModel.uiState.collectAsState().value.inBasketItems
                    LazyColumn(contentPadding = innerPadding) {
                        items(inBasketItems) { item ->
                            InBasketItemCard(item)
                        }
                    }
                }
            }
        }
    }
}