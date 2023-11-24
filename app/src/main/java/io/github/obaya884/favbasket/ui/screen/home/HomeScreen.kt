package io.github.obaya884.favbasket.ui.screen.home

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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.Screen
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val tabs = homeTabs(uiState.categories)

    val bottomSheetState =
        androidx.compose.material.rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
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
                Icon(Icons.Default.Settings, contentDescription = "Setting")
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
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "hide shopping cart",
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.List,
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
                    uiState = uiState,
                    bottomSheetState = bottomSheetState,
                    onClickGoShopping = {
                        navController.navigate(Screen.Shopping.route)
                    }
                )
            }
        ) {
            if (tabs.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                text = {
                                    Text(
                                        tab.title,
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
                            .fillMaxWidth()
                    ) { pagerIndex ->
                        val tab = tabs.getOrNull(pagerIndex)
                        val itemsForTab = when (tab) {
                            HomeTab.AllTab -> uiState.preparedItems
                            is HomeTab.CategoryTab -> uiState.preparedItems.filter { item ->
                                item.item.categoryId == tab.category.id
                            }

                            null -> emptyList()
                        }

                        MainTabItemList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            itemsForTab
                        ) { isInBasket, item ->
                            if (isInBasket) {
                                viewModel.removeFromBasket(item)
                            } else {
                                viewModel.addToBasket(item)
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

@Composable
fun MainTabItemList(
    modifier: Modifier,
    items: List<ItemWithCategory>,
    onItemAction: (Boolean, Item) -> Unit
) {
    Column {
        LazyColumn(
            modifier = modifier
        ) {
            items(items, key = { it.item.id }) { item ->
                PreparedItemRow(item.item) { isInBasket ->
                    onItemAction(isInBasket, item.item)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreenBottomSheetContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
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
                .background(MaterialTheme.colorScheme.primary)
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch { bottomSheetState.hide() }
                }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    imageVector = Icons.Default.Close,
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
                    imageVector = Icons.Default.Send,
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
            .padding(horizontal = 16.dp, vertical = 16.dp)
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
