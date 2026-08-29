package io.github.obaya884.rebuy.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.rebuy.R
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.screen.BottomNavigationBar
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val tabs = remember(uiState.categories) { HomeTab.homeTabs(uiState.categories) }

    val pagerState =
        rememberPagerState(initialPage = tabs.indexOf(HomeTab.All), pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    ReBuyAppScaffold(
        topBarTitle = stringResource(R.string.home_title),
        topBarActions = {
            IconButton(
                onClick = {
                    navController.navigate(Screen.ItemEdit.route)
                }
            ) {
                Icon(
                    painterResource(id = R.drawable.icon_shopping_bug),
                    contentDescription = null
                )
            }
            IconButton(
                onClick = {
                    navController.navigate(Screen.CategoryEdit.route)
                }
            ) {
                Icon(
                    painterResource(id = R.drawable.icon_category),
                    contentDescription = null
                )
            }
            IconButton(
                onClick = {
                    navController.navigate(Screen.Setting.route)
                }
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(navController, uiState.inShoppingListItems.size)
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
                modifier = Modifier.fillMaxWidth()
            ) { pagerIndex ->
                val tab =
                    tabs.getOrElse(pagerIndex) {
                        // index外でタブが存在しない場合はログを出力してAllを表示
                        HomeTab.All
                    }
                val itemsForTab = remember(pagerIndex, tabs, uiState) {
                    when (tab) {
                        HomeTab.InBasket -> uiState.inBasketItems
                        HomeTab.All -> uiState.items
                        is HomeTab.CategoryTab -> uiState.items.filter { item ->
                            item.item.categoryId == tab.category.id
                        }
                    }
                }
                HomePagerTabList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    navController = navController,
                    tab = tab,
                    items = itemsForTab,
                    onTapToAdd = { item ->
                        viewModel.addToBasket(item)
                    },
                    onTapToRemove = { item ->
                        viewModel.removeFromBasket(item)
                    },
                    scrollToAllTab = {
                        coroutineScope.launch {
                            // Ripple effect のために遅延を入れる
                            delay(200)
                            pagerState.scrollToPage(tabs.indexOf(HomeTab.All))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HomePagerTabList(
    modifier: Modifier,
    navController: NavController,
    tab: HomeTab,
    items: List<ItemWithCategory>,
    onTapToAdd: (Item) -> Unit,
    onTapToRemove: (Item) -> Unit,
    scrollToAllTab: () -> Unit
) {
    if (items.isNotEmpty()) {
        Column {
            LazyColumn(
                modifier = modifier
            ) {
                items(
                    items,
                    key = { it.item.id }
                ) { item ->
                    HomeListItemRow(
                        tab = tab,
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
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (tab) {
                    is HomeTab.All -> stringResource(id = R.string.home_no_item_message_all)
                    is HomeTab.CategoryTab -> stringResource(id = R.string.home_no_item_message_category)
                    is HomeTab.InBasket -> stringResource(id = R.string.home_no_item_message_in_basket)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            when (tab) {
                is HomeTab.All, is HomeTab.CategoryTab -> {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(48.dp),
                        onClick = {
                            navController.navigate(Screen.ItemEdit.route)
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_no_item_button)
                        )
                    }
                }

                is HomeTab.InBasket -> {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(48.dp),
                        onClick = {
                            scrollToAllTab()
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_no_item_button_shopping_list)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeListItemRow(
    tab: HomeTab,
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
                    modifier = Modifier.padding(bottom = 4.dp),
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Text(
                text = item.item.name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (item.item.lastBoughtAt != null) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(
                        id = R.string.home_last_bought_at,
                        item.item.lastBoughtAt.atZone(ZoneId.systemDefault()).format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                        )
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        if (item.item.status == ItemStatus.NO_DEAL) {
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
                Text(
                    text = if (tab is HomeTab.InBasket) {
                        stringResource(id = R.string.home_remove_item_button_from_shopping_list)
                    } else {
                        stringResource(id = R.string.home_remove_item_button)
                    }
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
