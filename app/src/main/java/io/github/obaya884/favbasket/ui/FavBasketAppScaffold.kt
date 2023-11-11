package io.github.obaya884.favbasket.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FavBasketAppScaffold(
    navController: NavController,
    topBarTitle: String,
    topBarActions: @Composable RowScope.() -> Unit = {},
    bottomSheetContent: @Composable () -> Unit,
    sheetPeekHeight: Dp,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Localized description"
                        )
                    }
                },
                actions = topBarActions
            )
        },
        drawerContent = {
            FavBasketAppDrawer(
                navController = navController,
                scaffoldState = scaffoldState,
                scope = scope
            )
        },
        sheetContent = { bottomSheetContent() },
        sheetShape = RoundedCornerShape(12.dp),
        sheetPeekHeight = sheetPeekHeight,
    ) { innerPadding ->
        content(innerPadding)
    }
}
