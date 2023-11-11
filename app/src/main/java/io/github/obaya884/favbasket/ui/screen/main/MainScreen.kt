package io.github.obaya884.favbasket.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.screen.main.widget.InBasketItemCard
import io.github.obaya884.favbasket.ui.screen.main.widget.PreparedItemCard

@Composable
fun MainScreen(navController: NavController) {
    val viewModel = hiltViewModel<MainViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    FavBasketAppScaffold(
        navController = navController,
        topBarTitle = "Home",
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            Text(
                text = "Prepared Item",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(MaterialTheme.colors.primary)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyColumn(
                modifier = Modifier
            ) {
                items(uiState.preparedItems) { item ->
                    PreparedItemCard(item) { isInBasket ->
                        if (isInBasket) {
                            viewModel.removeFromBasket(item)
                        } else {
                            viewModel.addToBasket(item)
                        }
                    }
                }
            }
            Text(
                text = "In Basket Item",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(MaterialTheme.colors.primary)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyColumn(
                modifier = Modifier
            ) {
                items(uiState.inBasketItems) { item ->
                    InBasketItemCard(item)
                }
            }
        }
    }
}
