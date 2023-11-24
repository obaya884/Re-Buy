package io.github.obaya884.favbasket.ui.screen.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.KeyboardArrowDown
import androidx.compose.material.icons.twotone.List
import androidx.compose.material.icons.twotone.Send
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemStatus
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavController) {
    val viewModel = hiltViewModel<MainViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val tabs = uiState.categories

    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    FavBasketAppScaffold(
        topBarTitle = "Home",
        topBarNavigationIcon = {
            IconButton(
                onClick = {
                    navController.navigate(Screen.Setting.route)
                }
            ) {
                Icon(Icons.TwoTone.Settings, contentDescription = "Setting")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.size(68.dp),
                onClick = {
                    coroutineScope.launch {
                        if (bottomSheetState.isVisible) {
                            bottomSheetState.hide()
                        } else {
                            bottomSheetState.show()
                        }
                    }
                }
            ) {
                // TODO: シートが開ききらないとアイコンが切り替わらない。押した直後に切り替えたい
                if (bottomSheetState.isVisible) {
                    Icon(
                        imageVector = Icons.TwoTone.KeyboardArrowDown,
                        contentDescription = "hide shopping cart",
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.TwoTone.List,
                        contentDescription = "show shopping cart",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetShape = RoundedCornerShape(topEnd = 12.dp, topStart = 12.dp),
            sheetContent = {
                MainScreenBottomSheetContent(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    bottomSheetState = bottomSheetState,
                    onClickGoShopping = {
                        navController.navigate(Screen.Shopping.route)
                    }
                )
            }
        ) {
            if (tabs.isNotEmpty()) {
                Column {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                text = {
                                    Text(
                                        tab.name,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(index)
                                    }
                                }
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) { page ->
                        Column {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                items(
                                    uiState.preparedItems.filter { item ->
                                        item.item.categoryId == tabs[page].id
                                    },
                                    key = { item -> item.item.id }
                                ) { item ->
                                    PreparedItemRow(item.item) { isInBasket ->
                                        if (isInBasket) {
                                            viewModel.removeFromBasket(item.item)
                                        } else {
                                            viewModel.addToBasket(item.item)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO: ローディング→追加してください系画面
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreenBottomSheetContent(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    bottomSheetState: ModalBottomSheetState,
    onClickGoShopping: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.primary)
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch { bottomSheetState.hide() }
                }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    imageVector = Icons.TwoTone.Close,
                    contentDescription = "close the bottom sheet",
                    tint = Color.White
                )
            }
            Text(
                text = "買い物リスト",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
            IconButton(
                enabled = uiState.inBasketItems.isNotEmpty(),
                onClick = {
                    onClickGoShopping()
                }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    imageVector = Icons.TwoTone.Send,
                    contentDescription = "出発",
                    tint = if (uiState.inBasketItems.isNotEmpty()) Color.White else Color.Gray
                )
            }
        }
        LazyColumn(
            modifier = Modifier
        ) {
            if (uiState.inBasketItems.isEmpty()) {
                item("empty_message") {
                    Text(text = "アイテムを追加してください")
                }
            } else {

                items(uiState.inBasketItems, key = { item -> item.item.id }) { item ->
                    InBasketItemRow(item.item)
                }
            }
        }
    }
}

@Composable
fun InBasketItemRow(item: Item) {
    Text(
        text = item.name,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

@Composable
fun PreparedItemRow(item: Item, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clickable(
                role = Role.Checkbox,
                onClick = {
                    onCheckedChange(item.status == ItemStatus.IN_BASKET)
                }
            )
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = item.name,
            modifier = Modifier.weight(1f)
        )

        Checkbox(
            checked = item.status == ItemStatus.IN_BASKET,
            onCheckedChange = null
        )
    }
}
