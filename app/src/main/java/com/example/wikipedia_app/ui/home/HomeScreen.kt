package com.example.wikipedia_app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.wikipedia_app.model.FeaturedArticle
import com.example.wikipedia_app.model.FeaturedImage
import com.example.wikipedia_app.model.OnThisDayItem
import com.example.wikipedia_app.model.TrendingArticleItem
import com.example.wikipedia_app.navigation.Screen
import com.example.wikipedia_app.ui.components.ErrorState
import com.example.wikipedia_app.ui.components.LoadingState
import com.example.wikipedia_app.ui.components.SectionLabel
import com.example.wikipedia_app.ui.viewmodels.TrendingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: TrendingViewModel
) {
    val featured by viewModel.featuredContent.collectAsState()
    val trending by viewModel.trendingArticles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Wiki-The-Racer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> LoadingState(Modifier.padding(padding))
            error != null && featured == null && trending.isEmpty() ->
                ErrorState(
                    message = error ?: "Something went wrong",
                    modifier = Modifier.padding(padding),
                    onRetry = { viewModel.refresh() }
                )
            else -> ExploreFeed(
                navController = navController,
                featuredArticle = featured?.today,
                pictureOfDay = featured?.image,
                onThisDay = featured?.onThisDay,
                trending = trending,
                contentPadding = padding
            )
        }
    }
}

@Composable
private fun ExploreFeed(
    navController: NavController,
    featuredArticle: FeaturedArticle?,
    pictureOfDay: FeaturedImage?,
    onThisDay: List<OnThisDayItem>?,
    trending: List<TrendingArticleItem>,
    contentPadding: PaddingValues
) {
    fun openArticle(title: String) =
        navController.navigate(Screen.Article.createRoute(title))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { GameCallout(onClick = { navController.navigate(Screen.Game.route) }) }

        featuredArticle?.let {
            item { FeaturedArticleCard(article = it, onClick = { openArticle(it.title) }) }
        }

        if (onThisDay != null && onThisDay.isNotEmpty()) {
            item { OnThisDayCard(events = onThisDay, onClick = ::openArticle) }
        }

        pictureOfDay?.let {
            item { PictureOfDayCard(image = it) }
        }

        if (trending.isNotEmpty()) {
            item {
                Text(
                    text = "Top read",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            items(trending.take(8)) { article ->
                TrendingRow(article = article, onClick = { openArticle(article.title) })
            }
        }
    }
}

@Composable
private fun WikiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    val base = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = base, shape = shape, colors = colors, elevation = elevation, content = { content() })
    } else {
        Card(modifier = base, shape = shape, colors = colors, elevation = elevation, content = { content() })
    }
}

@Composable
private fun GameCallout(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Casino,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Play the Wikirace",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Hop from one random article to another using only links.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun FeaturedArticleCard(article: FeaturedArticle, onClick: () -> Unit) {
    WikiCard(onClick = onClick) {
        Column {
            article.thumbnail?.let { thumb ->
                AsyncImage(
                    model = thumb.source,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    SectionLabel("Featured article", color = MaterialTheme.colorScheme.tertiary)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = article.title.replace("_", " "),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.extract,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4
                )
            }
        }
    }
}

@Composable
private fun PictureOfDayCard(image: FeaturedImage) {
    WikiCard {
        Column {
            image.thumbnail?.let { thumb ->
                AsyncImage(
                    model = thumb.source,
                    contentDescription = image.description?.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                SectionLabel("Picture of the day", color = MaterialTheme.colorScheme.secondary)
                image.description?.text?.let { desc ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
        }
    }
}

@Composable
private fun OnThisDayCard(events: List<OnThisDayItem>, onClick: (String) -> Unit) {
    WikiCard {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionLabel("On this day")
            Spacer(Modifier.height(12.dp))
            events.take(3).forEachIndexed { index, event ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = event.year.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = event.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        event.pages?.firstOrNull()?.let { page ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = page.title.replace("_", " "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onClick(page.title) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingRow(article: TrendingArticleItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "${article.rank}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(28.dp)
        )
        if (article.thumbnailUrl != null) {
            AsyncImage(
                model = article.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = article.title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title.replace("_", " "),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2
            )
            Text(
                text = "${formatViews(article.views)} views",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatViews(views: Int): String = when {
    views >= 1_000_000 -> String.format("%.1fM", views / 1_000_000.0)
    views >= 1_000 -> String.format("%.1fK", views / 1_000.0)
    else -> views.toString()
}
