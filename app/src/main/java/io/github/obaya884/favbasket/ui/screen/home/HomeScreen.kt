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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.R
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemStatus
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.Screen
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val tabs = homeTabs(uiState.categories)
    val needItemSnackbarMessage = stringResource(id = R.string.home_need_item_add_snack_bar)

    val bottomSheetState =
        androidx.compose.material.rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden
        )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val shouldHideSheetWithRecomposition by remember { mutableStateOf(false) }

    // TODO: navigationの引数でフラグを受け取って、trueならシートを閉じるように変更
    LaunchedEffect(Unit) {
        if (shouldHideSheetWithRecomposition) {
            bottomSheetState.hide()
        }
    }

    FavBasketAppScaffold(
        topBarTitle = stringResource(R.string.home_title),
        topBarNavigationIcon = {
            IconButton(
                onClick = {
                    navController.navigate(Screen.Setting.route)
                }
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Setting"
                )
            }
        },
        topBarActions = {
            IconButton(
                onClick = {
                    navController.navigate(Screen.ItemEdit.route)
                }
            ) {
                Icon(
                    painterResource(id = R.drawable.icon_shopping_bug),
                    contentDescription = "Item edit"
                )
            }
            IconButton(
                onClick = {
                    navController.navigate(Screen.CategoryEdit.route)
                }
            ) {
                Icon(
                    painterResource(id = R.drawable.icon_category),
                    contentDescription = "Category edit"
                )
            }
            IconButton(
                onClick = {
                    if (uiState.inBasketItems.isNotEmpty()) {
                        navController.navigate(Screen.Shopping.route)
                    } else {
                        // TODO: LaunchedEffectに移動できるかも
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = needItemSnackbarMessage,
                                withDismissAction = true
                            )
                        }
                    }
                }
            ) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = "Setting")
            }
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            if (!bottomSheetState.isVisible) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            bottomSheetState.show()
                        }
                    }
                ) {
                    Icon(
                        painterResource(id = R.drawable.icon_check_list),
                        contentDescription = "show shopping cart"
                    )
                }
            } else {
                ExtendedFloatingActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "show shopping cart"
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(id = R.string.home_extend_floating_action_button)
                        )
                    },
                    onClick = {
                        if (uiState.inBasketItems.isNotEmpty()) {
                            navController.navigate(Screen.Shopping.route)
                        } else {
                            coroutineScope.launch {
                                bottomSheetState.hide()
                                snackbarHostState.showSnackbar(
                                    message = needItemSnackbarMessage,
                                    withDismissAction = true
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetShape = RoundedCornerShape(topEnd = 12.dp, topStart = 12.dp),
            sheetContent = {
                MainScreenBottomSheetContent(
                    inBasketItems = uiState.inBasketItems,
                    bottomSheetState = bottomSheetState
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
                        val itemsForTab = when (val tab = tabs.getOrNull(pagerIndex)) {
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
                HomeListItemRow(item.item) { isInBasket ->
                    onItemAction(isInBasket, item.item)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreenBottomSheetContent(
    inBasketItems: List<ItemWithCategory>,
    bottomSheetState: ModalBottomSheetState
) {
    // TODO: 高さが大きくなった場合にAppBarまで突き抜けてしまう
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 380.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch { bottomSheetState.hide() }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "close the bottom sheet",
                    tint = Color.White
                )
            }
            Text(
                text = stringResource(id = R.string.home_bottom_sheet_title),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
        ) {
            if (inBasketItems.isEmpty()) {
                item("empty_message") {
                    Text(
                        text = stringResource(id = R.string.home_bottom_sheet_empty_message)
                    )
                }
            } else {
                items(
                    inBasketItems,
                    key = { item -> item.item.id }
                ) { item ->
                    BottomSheetListItemRow(item)
                }
            }
        }
    }
}

@Composable
fun BottomSheetListItemRow(item: ItemWithCategory) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = item.item.name,
            modifier = Modifier.padding(end = 16.dp)
        )
        item.category?.name?.let {
            Text(text = it)
        }
    }
}

@Composable
fun HomeListItemRow(item: Item, onCheckedChange: (Boolean) -> Unit) {
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
        Checkbox(
            modifier = Modifier.padding(end = 16.dp),
            checked = item.status == ItemStatus.IN_BASKET,
            onCheckedChange = null
        )
        Text(
            text = item.name,
            modifier = Modifier.weight(1f)
        )

    }
}
