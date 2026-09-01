package io.github.obaya884.rebuy.ui.screen.pool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.formatMonthDay
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import io.github.obaya884.rebuy.ui.theme.tabularNumbers
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * プール（画面 01）。**唯一の常駐画面**で、全品目の一覧と買い物の起点。
 *
 * 行タップはカゴの出し入れで、**カゴに入れても行は動かない**（一覧の上に寄せない）。
 * 押した場所がそのまま結果になるほうが、連続して触るときに迷わないため。
 *
 * **行の長押し（→ 06 品目編集シート）はまだ無い**（F-007）。ほかに行き先の画面が
 * 揃っていない導線は旧画面へ暫定で繋いである——`// 暫定:` で grep できる。
 */
@Composable
fun PoolScreen(
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = koinViewModel<PoolViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    ReBuyAppScaffold(
        topBarTitle = stringResource(Res.string.pool_title),
        topBarActions = {
            Text(
                text = stringResource(Res.string.pool_total_count, uiState.totalCount),
                style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                color = ReBuyTheme.colors.muted
            )
            IconButton(
                modifier = Modifier.testTag(TestTags.POOL_ADD_BUTTON),
                // 暫定: 02 登録シートは F-006
                onClick = { navigator.navigate(Screen.ItemEdit) }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
            IconButton(
                modifier = Modifier.testTag(TestTags.POOL_SETTINGS_BUTTON),
                onClick = { navigator.navigate(Screen.Setting) }
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            FilterChips(
                categories = uiState.categories,
                destinations = uiState.destinations,
                selectedCategoryId = uiState.selectedCategoryId,
                destinationFilter = uiState.destinationFilter,
                isNoFilter = uiState.isNoFilter,
                onSelectAll = viewModel::clearFilters,
                onSelectCategory = viewModel::selectCategory,
                onSelectDestination = viewModel::selectDestination
            )

            when {
                uiState.isEmpty -> EmptyMessage(
                    title = stringResource(Res.string.pool_empty_title),
                    message = stringResource(Res.string.pool_empty_message),
                    modifier = Modifier.weight(1f)
                )

                uiState.isFilteredEmpty -> EmptyMessage(
                    title = stringResource(Res.string.pool_filtered_empty),
                    message = null,
                    modifier = Modifier.weight(1f)
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.visibleItems, key = { it.item.id }) { poolItem ->
                        PoolRow(poolItem = poolItem, onTap = { viewModel.toggleBasket(poolItem.item) })
                    }
                }
            }

            StartShoppingButton(
                basketCount = uiState.basketCount,
                enabled = uiState.canStartShopping,
                // 暫定: 03 買い物開始シートは F-008
                onClick = { navigator.navigate(Screen.Shopping) }
            )
        }
    }
}

@Composable
private fun FilterChips(
    categories: List<Category>,
    destinations: List<Destination>,
    selectedCategoryId: Int?,
    destinationFilter: DestinationFilter,
    isNoFilter: Boolean,
    onSelectAll: () -> Unit,
    onSelectCategory: (Int) -> Unit,
    onSelectDestination: (DestinationFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        FilterChip(
            selected = isNoFilter,
            onClick = onSelectAll,
            label = { Text(stringResource(Res.string.pool_filter_all)) },
            modifier = Modifier.testTag(TestTags.POOL_CHIP_ALL)
        )
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onSelectCategory(category.id) },
                label = { Text(category.name) },
                modifier = Modifier.testTag(TestTags.poolCategoryChip(category.id))
            )
        }
        destinations.forEach { destination ->
            FilterChip(
                selected = destinationFilter == DestinationFilter.Only(destination.id),
                onClick = { onSelectDestination(DestinationFilter.Only(destination.id)) },
                // 🏬 は表示のときに前置する。名前の一部ではない（画面 01）
                label = { Text(stringResource(Res.string.pool_destination_prefix, destination.name)) },
                modifier = Modifier.testTag(TestTags.poolDestinationChip(destination.id))
            )
        }
        FilterChip(
            selected = destinationFilter == DestinationFilter.Anywhere,
            onClick = { onSelectDestination(DestinationFilter.Anywhere) },
            label = { Text(stringResource(Res.string.pool_filter_anywhere)) },
            modifier = Modifier.testTag(TestTags.POOL_CHIP_ANYWHERE)
        )
    }
}

/**
 * 一覧の 1 行。カゴ入りは**面の色**で示す（トグルの ✓ と合わせて 2 通りで分かるように）。
 */
@Composable
private fun PoolRow(poolItem: PoolItem, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        colors = CardDefaults.cardColors(
            containerColor = if (poolItem.isInBasket) {
                ReBuyTheme.colors.accentSoft
            } else {
                ReBuyTheme.colors.card
            }
        ),
        modifier = Modifier.fillMaxWidth().testTag(TestTags.poolRow(poolItem.item.id))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poolItem.item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ReBuyTheme.colors.ink
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    poolItem.category?.let { RowMetaText(it.name) }
                    poolItem.destination?.let {
                        RowMetaText(stringResource(Res.string.pool_destination_prefix, it.name))
                    }
                    Text(
                        text = poolItem.item.lastBoughtAt
                            ?.let { stringResource(Res.string.pool_last_bought_at, formatMonthDay(it)) }
                            ?: stringResource(Res.string.pool_last_bought_at_never),
                        style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                        color = ReBuyTheme.colors.muted
                    )
                }
            }
            BasketCheckMark(isInBasket = poolItem.isInBasket)
        }
    }
}

@Composable
private fun RowMetaText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = ReBuyTheme.colors.muted
    )
}

@Composable
private fun BasketCheckMark(isInBasket: Boolean) {
    if (isInBasket) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = ReBuyTheme.colors.accent
        )
    }
}

@Composable
private fun EmptyMessage(title: String, message: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = ReBuyTheme.colors.ink
        )
        message?.let {
            Text(
                text = it,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = ReBuyTheme.colors.muted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StartShoppingButton(basketCount: Int, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag(TestTags.POOL_START_SHOPPING_BUTTON)
    ) {
        Text(stringResource(Res.string.pool_start_shopping))
        if (basketCount > 0) {
            Badge(modifier = Modifier.padding(start = 8.dp)) {
                Text(basketCount.toString(), style = MaterialTheme.typography.labelMedium.tabularNumbers())
            }
        }
    }
}
