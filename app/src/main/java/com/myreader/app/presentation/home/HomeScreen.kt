package com.myreader.app.presentation.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myreader.app.domain.model.*
import com.myreader.app.presentation.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onBookClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onLibraryClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            HomeTopBar(
                userName = state.currentUser?.displayName ?: "Reader",
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick,
            )
        },
        bottomBar = {
            HomeBottomBar(
                onHomeClick = {},
                onLibraryClick = onLibraryClick,
                onProfileClick = onProfileClick,
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            HomeLoadingState(Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Featured Banner
                item {
                    FeaturedBanner(
                        books = state.featuredBooks,
                        onBookClick = onBookClick,
                    )
                }

                // Categories
                item {
                    SectionTitle(title = "Categories", showSeeAll = false)
                    CategoryRow(onCategoryClick = onCategoryClick)
                }

                // Continue Reading
                if (state.continueReading.isNotEmpty()) {
                    item { SectionTitle(title = "Continue Reading", showSeeAll = true, onSeeAll = onLibraryClick) }
                    item {
                        HorizontalBookList(
                            books = state.continueReading,
                            onBookClick = onBookClick,
                            showProgress = true,
                        )
                    }
                }

                // Popular Books
                item { SectionTitle(title = "Popular Books", showSeeAll = true) }
                item { HorizontalBookList(books = state.popularBooks, onBookClick = onBookClick) }

                // Newly Added
                item { SectionTitle(title = "Newly Added", showSeeAll = true) }
                item { HorizontalBookList(books = state.newlyAdded, onBookClick = onBookClick) }

                // Top Rated
                item { SectionTitle(title = "Top Rated", showSeeAll = true) }
                item { HorizontalBookList(books = state.topRated, onBookClick = onBookClick) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    userName: String,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Good day, ${userName.split(" ").first()}! 👋",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "What will you read today?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, "Search", modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Outlined.AccountCircle, "Profile", modifier = Modifier.size(26.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedBanner(books: List<Book>, onBookClick: (String) -> Unit) {
    if (books.isEmpty()) return
    val pagerState = rememberPagerState { books.size }

    // Auto-scroll
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3500)
            val next = (pagerState.currentPage + 1) % books.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) { page ->
            FeaturedBannerItem(book = books[page], onClick = { onBookClick(books[page].id) })
        }

        // Pager indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(books.size) { idx ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == idx) 24.dp else 6.dp, 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == idx) Emerald400
                            else White.copy(alpha = 0.5f)
                        )
                )
            }
        }
    }
}

@Composable
private fun FeaturedBannerItem(book: Book, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY = 100f,
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Emerald500),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = "Featured",
                    style = MaterialTheme.typography.labelSmall,
                    color = White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
                color = White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                color = White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun CategoryRow(onCategoryClick: (String) -> Unit) {
    val categories = BookCategory.entries
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories) { cat ->
            CategoryChip(category = cat, onClick = { onCategoryClick(cat.name) })
        }
    }
}

@Composable
private fun CategoryChip(category: BookCategory, onClick: () -> Unit) {
    val (icon, color) = remember(category) { categoryIconAndColor(category) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = category.displayNameEn,
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

private fun categoryIconAndColor(cat: BookCategory) = when (cat) {
    BookCategory.ISLAMIC      -> Pair(Icons.Filled.Star, Emerald600)
    BookCategory.NOVELS       -> Pair(Icons.Filled.AutoStories, Gold600)
    BookCategory.EDUCATIONAL  -> Pair(Icons.Filled.School, InfoBlue)
    BookCategory.SCIENCE      -> Pair(Icons.Filled.Science, Color(0xFF8B5CF6))
    BookCategory.HISTORY      -> Pair(Icons.Filled.HistoryEdu, Color(0xFFDC2626))
    BookCategory.TECHNOLOGY   -> Pair(Icons.Filled.Code, Color(0xFF0EA5E9))
    BookCategory.BIOGRAPHY    -> Pair(Icons.Filled.Person, Color(0xFFF97316))
    BookCategory.CHILDREN     -> Pair(Icons.Filled.ChildCare, Color(0xFFEC4899))
    BookCategory.SELF_DEVELOPMENT -> Pair(Icons.Filled.TrendingUp, SuccessGreen)
    BookCategory.OTHER        -> Pair(Icons.Filled.Category, NeutralDark)
}

@Composable
fun SectionTitle(
    title: String,
    showSeeAll: Boolean = true,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (showSeeAll) {
            TextButton(onClick = { onSeeAll?.invoke() }) {
                Text("See All", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun HorizontalBookList(
    books: List<Book>,
    onBookClick: (String) -> Unit,
    showProgress: Boolean = false,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(book = book, onClick = { onBookClick(book.id) })
        }
    }
}

@Composable
fun BookCard(book: Book, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column {
            // Cover
            Box(modifier = Modifier.height(190.dp).fillMaxWidth()) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Format badge
                Surface(
                    modifier = Modifier.padding(6.dp).align(Alignment.TopEnd),
                    color = Emerald600.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        book.fileFormat.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
            // Info
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star, null,
                        tint = Gold500, modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "%.1f".format(book.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    onHomeClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = onHomeClick,
            icon = { Icon(Icons.Filled.Home, null) },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onLibraryClick,
            icon = { Icon(Icons.Outlined.LibraryBooks, null) },
            label = { Text("Library") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onProfileClick,
            icon = { Icon(Icons.Outlined.Person, null) },
            label = { Text("Profile") },
        )
    }
}

@Composable
private fun HomeLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Emerald600)
        Spacer(Modifier.height(16.dp))
        Text("Loading your library...", style = MaterialTheme.typography.bodyMedium)
    }
}
