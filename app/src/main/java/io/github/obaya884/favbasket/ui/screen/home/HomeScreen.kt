package io.github.obaya884.favbasket.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.R
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemStatus
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val tabs = HomeTab.homeTabs(uiState.categories)
    val needItemSnackbarMessage = stringResource(id = R.string.home_need_item_add_snack_bar)

    val pagerState =
        rememberPagerState(initialPage = tabs.indexOf(HomeTab.All), pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    FavBasketAppScaffold(
        topBarTitle = stringResource(R.string.home_title),
        topBarNavigationIcon = {
            IconButton(
                onClick = {
                    navController.navigate(Screen.Setting.route)
                }
            ) {
                Icon(
                    Icons.Outlined.Settings,
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
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
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
                    val backgroundColor =
                        if (index == pagerState.currentPage) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.background
                    Tab(
                        text = {
                            Surface(
                                modifier = Modifier
                                    .background(
                                        color = backgroundColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    tab.title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.background(color = backgroundColor)
                                )
                            }

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
                    HomeTab.InBasket -> uiState.inBasketItems
                    HomeTab.All -> uiState.preparedItems
                    is HomeTab.CategoryTab -> uiState.preparedItems.filter { item ->
                        item.item.categoryId == tab.category.id
                    }

                    null -> emptyList()
                }

                MainTabItemList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    items = itemsForTab,
                    onTapToAdd = { item ->
                        viewModel.addToBasket(item)
                    },
                    onTapToRemove = { item ->
                        viewModel.removeFromBasket(item)
                    }
                )
            }
        }
    }
}

@Composable
fun MainTabItemList(
    modifier: Modifier,
    items: List<ItemWithCategory>,
    onTapToAdd: (Item) -> Unit,
    onTapToRemove: (Item) -> Unit
) {
    if (items.isNotEmpty()) {

        Column {
            LazyColumn(
                modifier = modifier
            ) {
                items(items, key = { it.item.id }) { item ->
                    HomeListItemRow(
                        item = item,
                        onTapToAdd = {
                            onTapToAdd(item.item)
                        },
                        onTapToRemove = {
                            onTapToRemove(item.item)
                        }
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.home_no_item_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        // TODO: アイテム編集画面への遷移
    }
}

@Composable
fun HomeListItemRow(
    item: ItemWithCategory,
    onTapToAdd: () -> Unit,
    onTapToRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            item.category?.name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Text(
                text = item.item.name,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (item.item.status != ItemStatus.IN_BASKET) {
            FilledTonalButton(
                onClick = {
                    onTapToAdd()
                }
            ) {
                Text(text = stringResource(id = R.string.home_add_item_button))
            }
        } else {
            OutlinedButton(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                onClick = {
                    onTapToRemove()
                }
            ) {
                Text(text = stringResource(id = R.string.home_remove_item_button))
            }
        }
    }
    Divider(
        modifier = Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
