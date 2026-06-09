package com.example.wikipedia_app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.wikipedia_app.model.FeaturedArticle
import com.example.wikipedia_app.model.FeaturedArticleResponse
import com.example.wikipedia_app.model.FeaturedImage
import com.example.wikipedia_app.model.OnThisDayItem
import com.example.wikipedia_app.model.TrendingArticleItem
import com.example.wikipedia_app.navigation.Screen
import com.example.wikipedia_app.ui.theme.*
import com.example.wikipedia_app.ui.viewmodels.TrendingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: TrendingViewModel
) {
    val featuredContent by viewModel.featuredContent.collectAsState()
    val trendingArticles by viewModel.trendingArticles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "WIKI-VOYAGE",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CreamOffWhite,
                            fontFamily = FontFamily.Serif,
                            fontSize = 32.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealCyan,
                    titleContentColor = CreamOffWhite
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Game.route) },
                containerColor = TealCyan,
                contentColor = CreamOffWhite
            ) {
                Icon(Icons.Default.Gamepad, contentDescription = "Hyperlink Game")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundBeige)
                    .padding(padding)
            ) {
                // Today's Featured Article
                featuredContent?.today?.let { article ->
                    item {
                        FeaturedArticleCard(
                            title = "Today's Featured Article",
                            article = article,
                            onClick = {
                                navController.navigate(Screen.Article.createRoute(article.title))
                            }
                        )
                    }
                }

                // Picture of the Day (File namespace — display only, no navigation)
                featuredContent?.image?.let { image ->
                    item {
                        PictureOfTheDayCard(image = image)
                    }
                }

                // On This Day
                featuredContent?.onThisDay?.let { events ->
                    item {
                        OnThisDayCard(
                            events = events,
                            onClick = { title ->
                                navController.navigate(Screen.Article.createRoute(title))
                            }
                        )
                    }
                }

                // Top Read Articles
                if (trendingArticles.isNotEmpty()) {
                    item {
                        Text(
                            text = "Top Read Articles",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkBrown
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(trendingArticles.take(5)) { article ->
                        TrendingArticleCard(
                            article = article,
                            onClick = {
                                navController.navigate(Screen.Article.createRoute(article.title))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedArticleCard(
    title: String,
    article: FeaturedArticle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = CreamOffWhite.copy(alpha = 0.9f)
        )
    ) {
        Column {
            article.thumbnail?.let { thumbnail ->
                AsyncImage(
                    model = thumbnail.source,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TealCyan
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkBrown
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.extract,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = DarkBrown
                    ),
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
fun PictureOfTheDayCard(image: FeaturedImage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CreamOffWhite.copy(alpha = 0.9f)
        )
    ) {
        Column {
            image.thumbnail?.let { thumbnail ->
                AsyncImage(
                    model = thumbnail.source,
                    contentDescription = image.description?.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Picture of the Day",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TealCyan
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = image.title
                        .removePrefix("File:")
                        .substringBeforeLast(".")
                        .replace("_", " "),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkBrown
                    )
                )
                image.description?.text?.let { description ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = DarkBrown
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun OnThisDayCard(
    events: List<OnThisDayItem>,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CreamOffWhite.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "On This Day",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TealCyan
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            events.take(3).forEach { event ->
                Text(
                    text = "${event.year}: ${event.text}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = DarkBrown
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                event.pages?.firstOrNull()?.let { page ->
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TealCyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { onClick(page.title) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingArticleCard(
    article: TrendingArticleItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = CreamOffWhite.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            article.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkBrown
                    )
                )
                Text(
                    text = "${article.views} views",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TealCyan
                    )
                )
            }
            Text(
                text = "#${article.rank}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TealCyan
                )
            )
        }
    }
}
