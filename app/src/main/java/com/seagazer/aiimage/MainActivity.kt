package com.seagazer.aiimage

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.seagazer.aiimage.domain.ResultDetail
import com.seagazer.aiimage.ui.components.EtherealBottomBar
import com.seagazer.aiimage.ui.components.EtherealTab
import com.seagazer.aiimage.ui.components.LumiaGeneratingDialog
import com.seagazer.aiimage.ui.components.ResultTabHighlight
import com.seagazer.aiimage.ui.create.CreateScreen
import com.seagazer.aiimage.ui.dialogs.GenerationFailedDialog
import com.seagazer.aiimage.ui.dialogs.NetworkErrorDialog
import com.seagazer.aiimage.ui.dialogs.EnterPrivatePasswordDialog
import com.seagazer.aiimage.ui.dialogs.SetPrivatePasswordDialog
import com.seagazer.aiimage.ui.gallery.GalleryScreen
import com.seagazer.aiimage.ui.gallery.PrivateSpaceScreen
import com.seagazer.aiimage.ui.gallery.RecycleBinScreen
import com.seagazer.aiimage.ui.result.ResultDetailScreen
import com.seagazer.aiimage.ui.settings.SettingsScreen
import com.seagazer.aiimage.ui.theme.AIImageTheme
import com.seagazer.aiimage.util.AppLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(128L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLocale.applyFromPreferences(applicationContext)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                val vm: MainViewModel = viewModel()
                val themeDark by vm.themeDark.collectAsStateWithLifecycle()
                AIImageTheme(darkTheme = themeDark) {
                    EtherealApp(vm = vm)
                }
            }
        }
    }
}

@Composable
private fun EtherealApp(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(EtherealTab.Create) }
    val result by vm.resultDetail.collectAsStateWithLifecycle()
    val resultTabGallery by vm.resultBottomTabGallery.collectAsStateWithLifecycle()
    val prompt by vm.prompt.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    val generationProgress by vm.generationProgressPercent.collectAsStateWithLifecycle()
    val galleryItems by vm.gallery.collectAsStateWithLifecycle()
    val recycleBinItems by vm.recycleBin.collectAsStateWithLifecycle()
    val themeDark by vm.themeDark.collectAsStateWithLifecycle()
    val resultFromGallery by vm.resultFromGallery.collectAsStateWithLifecycle()
    val resultFromPrivateSpace by vm.resultFromPrivateSpace.collectAsStateWithLifecycle()
    val privateSpaceItems by vm.privateSpace.collectAsStateWithLifecycle()
    val comfyUrl by vm.comfyBaseUrl.collectAsStateWithLifecycle()
    val dailyGenUsed by vm.dailyGenerationsUsed.collectAsStateWithLifecycle()
    val dailyGenCap = vm.dailyGenerationsCap
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val galleryLazyGridState = rememberLazyGridState()
    var resultImageFullscreen by remember { mutableStateOf(false) }
    var showRecycleBin by rememberSaveable { mutableStateOf(false) }
    var showPrivateSpace by rememberSaveable { mutableStateOf(false) }
    var galleryScrollToItemId by remember { mutableStateOf<String?>(null) }
    var pendingPrivateAction by remember { mutableStateOf<PrivateAction?>(null) }

    LaunchedEffect(result) {
        if (result == null) resultImageFullscreen = false
    }

    BackHandler(enabled = result != null) { vm.clearResult() }
    BackHandler(enabled = showPrivateSpace && result == null) { showPrivateSpace = false }
    BackHandler(enabled = showRecycleBin && result == null) { showRecycleBin = false }

    LaunchedEffect(tab, result) {
        if (tab == EtherealTab.Settings && result == null) {
            vm.refreshDailyGenerationsFromPrefs()
        }
    }

    LaunchedEffect(Unit) {
        vm.dailyLimitExceeded.collect {
            Toast.makeText(
                context,
                context.getString(R.string.daily_generation_limit_reached),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val onPrivateActionCompleted: (PrivateAction?) -> Unit = { action ->
        when (action) {
            is PrivateAction.MoveToPrivate -> {
                vm.moveGalleryItemToPrivateSpace(action.itemId)
                Toast.makeText(context, context.getString(R.string.moved_to_private_space), Toast.LENGTH_SHORT).show()
            }
            PrivateAction.OpenPrivateSpace -> showPrivateSpace = true
            null -> {}
        }
        pendingPrivateAction = null
    }

    fun requestPrivateAction(action: PrivateAction) {
        pendingPrivateAction = action
    }

    AppDialogHost(
        vm = vm,
        generating = generating && result == null,
        generationProgress = generationProgress,
        pendingPrivateAction = pendingPrivateAction,
        onPrivateActionCompleted = onPrivateActionCompleted,
        onDismissPrivateAction = { pendingPrivateAction = null },
        onRetryGeneration = {
            scope.launch {
                delay(300)
                vm.generateArt { d -> if (d != null) vm.openResultFromGeneration(d) }
            }
        },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (result == null || !resultImageFullscreen) {
                EtherealBottomBar(
                    selected = tab,
                    resultHighlight = if (result != null) {
                        if (resultTabGallery) ResultTabHighlight.Gallery else ResultTabHighlight.Create
                    } else {
                        null
                    },
                    onSelect = { t ->
                        if (result != null) vm.clearResult()
                        if (t != EtherealTab.Gallery) {
                            showRecycleBin = false
                            showPrivateSpace = false
                            galleryScrollToItemId = null
                        }
                        tab = t
                    },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = result, label = "root") { res ->
                when (res) {
                    null -> TabContent(
                        tab = tab,
                        vm = vm,
                        prompt = prompt,
                        generating = generating,
                        galleryItems = galleryItems,
                        recycleBinItems = recycleBinItems,
                        privateSpaceItems = privateSpaceItems,
                        showPrivateSpace = showPrivateSpace,
                        showRecycleBin = showRecycleBin,
                        galleryLazyGridState = galleryLazyGridState,
                        galleryScrollToItemId = galleryScrollToItemId,
                        themeDark = themeDark,
                        comfyUrl = comfyUrl,
                        dailyGenUsed = dailyGenUsed,
                        dailyGenCap = dailyGenCap,
                        onTabChange = { tab = it },
                        onShowRecycleBin = { showRecycleBin = it },
                        onShowPrivateSpace = { showPrivateSpace = it },
                        onScrollToItemConsumed = { galleryScrollToItemId = null },
                        onScrollToItem = { galleryScrollToItemId = it },
                        onRequestPrivateAction = ::requestPrivateAction,
                    )

                    else -> ResultPane(
                        detail = res,
                        vm = vm,
                        fromGallery = resultFromGallery,
                        isFromPrivateSpace = resultFromPrivateSpace,
                        onDismissResult = { vm.clearResult() },
                        onImageFullscreenChange = { resultImageFullscreen = it },
                        onRequestMoveToPrivate = { id ->
                            if (!vm.isPrivatePasswordSet()) {
                                requestPrivateAction(PrivateAction.MoveToPrivate(id))
                            } else {
                                vm.moveGalleryItemToPrivateSpace(id)
                                Toast.makeText(context, context.getString(R.string.moved_to_private_space), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRequestRestoreFromPrivate = { id ->
                            vm.restorePrivateSpaceItemToGallery(id)
                            Toast.makeText(context, context.getString(R.string.restored_from_private_space), Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDialogHost(
    vm: MainViewModel,
    generating: Boolean,
    generationProgress: Int?,
    pendingPrivateAction: PrivateAction?,
    onPrivateActionCompleted: (PrivateAction?) -> Unit,
    onDismissPrivateAction: () -> Unit,
    onRetryGeneration: () -> Unit,
) {
    val netErr by vm.showNetworkError.collectAsStateWithLifecycle()
    val netErrDetail by vm.networkErrorDetail.collectAsStateWithLifecycle()
    val genFail by vm.showGenerationFailed.collectAsStateWithLifecycle()
    val genFailDetail by vm.generationFailedDetail.collectAsStateWithLifecycle()

    NetworkErrorDialog(
        visible = netErr,
        detail = netErrDetail,
        onRetry = { vm.showNetworkError(false) },
        onCancel = { vm.showNetworkError(false) },
    )
    GenerationFailedDialog(
        visible = genFail,
        detail = genFailDetail,
        onTryAgain = {
            vm.showGenerationFailed(false)
            onRetryGeneration()
        },
        onDismiss = { vm.showGenerationFailed(false) },
    )
    LumiaGeneratingDialog(
        visible = generating,
        title = stringResource(R.string.generating_title),
        progressPercent = generationProgress,
        onCancel = { vm.cancelGeneration() },
    )

    val showSet = pendingPrivateAction != null && !vm.isPrivatePasswordSet()
    val showEnter = pendingPrivateAction != null && vm.isPrivatePasswordSet()

    SetPrivatePasswordDialog(
        visible = showSet,
        onConfirm = { password ->
            vm.setPrivatePassword(password)
            onPrivateActionCompleted(pendingPrivateAction)
        },
        onDismiss = onDismissPrivateAction,
    )
    EnterPrivatePasswordDialog(
        visible = showEnter,
        onConfirm = { password ->
            if (vm.verifyPrivatePassword(password)) {
                onPrivateActionCompleted(pendingPrivateAction)
            }
        },
        onDismiss = onDismissPrivateAction,
    )
}

@Composable
private fun TabContent(
    tab: EtherealTab,
    vm: MainViewModel,
    prompt: String,
    generating: Boolean,
    galleryItems: List<com.seagazer.aiimage.domain.GalleryItem>,
    recycleBinItems: List<com.seagazer.aiimage.domain.GalleryItem>,
    privateSpaceItems: List<com.seagazer.aiimage.domain.GalleryItem>,
    showPrivateSpace: Boolean,
    showRecycleBin: Boolean,
    galleryLazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    galleryScrollToItemId: String?,
    themeDark: Boolean,
    comfyUrl: String,
    dailyGenUsed: Int,
    dailyGenCap: Int,
    onTabChange: (EtherealTab) -> Unit,
    onShowRecycleBin: (Boolean) -> Unit,
    onShowPrivateSpace: (Boolean) -> Unit,
    onScrollToItemConsumed: () -> Unit,
    onScrollToItem: (String) -> Unit,
    onRequestPrivateAction: (PrivateAction) -> Unit,
) {
    val context = LocalContext.current
    val appLanguage by vm.appLanguage.collectAsStateWithLifecycle()
    when (tab) {
        EtherealTab.Create -> CreateScreen(
            viewModel = vm,
            prompt = prompt,
            generating = generating,
            onPromptChange = vm::setPrompt,
            onGenerate = {
                vm.generateArt { detail ->
                    if (detail != null) {
                        vm.openResultFromGeneration(detail)
                        onTabChange(EtherealTab.Gallery)
                    }
                }
            },
        )

        EtherealTab.Gallery -> when {
            showPrivateSpace -> PrivateSpaceScreen(
                items = privateSpaceItems,
                onBack = { onShowPrivateSpace(false) },
                onOpenItem = { vm.openResultFromPrivateSpace(it) },
            )
            showRecycleBin -> RecycleBinScreen(
                items = recycleBinItems,
                onBack = { onShowRecycleBin(false) },
                onRestore = {
                    vm.restoreRecycleBinItem(it.id)
                    onScrollToItem(it.id)
                },
                onPermanentDelete = { vm.permanentlyDeleteRecycleBinItem(it.id) },
            )
            else -> GalleryScreen(
                items = galleryItems,
                lazyGridState = galleryLazyGridState,
                onOpenItem = { vm.openResultFromGallery(it) },
                onOpenRecycleBin = { onShowRecycleBin(true) },
                onOpenPrivateSpace = {
                    onRequestPrivateAction(PrivateAction.OpenPrivateSpace)
                },
                scrollToItemId = galleryScrollToItemId,
                onScrollToItemConsumed = onScrollToItemConsumed,
            )
        }

        EtherealTab.Settings -> SettingsScreen(
            themeDark = themeDark,
            appLanguage = appLanguage,
            comfyBaseUrl = comfyUrl,
            dailyGenerationsUsed = dailyGenUsed,
            dailyGenerationsMax = dailyGenCap,
            onThemeDarkChange = vm::setThemeDark,
            onAppLanguageChange = change@{ option ->
                if (option == appLanguage) return@change
                vm.setAppLanguage(option)
                val act = context as? Activity ?: return@change
                act.window.decorView.post {
                    if (!act.isFinishing) act.recreate()
                }
            },
            onComfyUrlChange = vm::setComfyBaseUrl,
            onClearCache = { vm.clearAppCaches() },
        )
    }
}

@Composable
private fun ResultPane(
    detail: ResultDetail,
    vm: MainViewModel,
    fromGallery: Boolean,
    isFromPrivateSpace: Boolean,
    onDismissResult: () -> Unit,
    onImageFullscreenChange: (Boolean) -> Unit,
    onRequestMoveToPrivate: (String) -> Unit,
    onRequestRestoreFromPrivate: (String) -> Unit,
) {
    val context = LocalContext.current
    var loadingImage by remember { mutableStateOf(false) }
    LaunchedEffect(detail.id) {
        onImageFullscreenChange(false)
    }
    LaunchedEffect(detail.imageUrl) {
        loadingImage = true
        delay(400)
        loadingImage = false
    }
    fun toastSave(ok: Boolean, export: Boolean) {
        val resId = when {
            !ok -> R.string.save_to_album_failed
            export -> R.string.exported_to_photos
            else -> R.string.saved_to_photos
        }
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
    }
    ResultDetailScreen(
        detail = detail,
        fromGallery = fromGallery,
        loadingImage = loadingImage,
        onBack = onDismissResult,
        onImageViewerVisibilityChanged = onImageFullscreenChange,
        onSaveToGallery = {
            val ok = vm.saveResultToPublicAlbum(detail.imageUrl)
            toastSave(ok, export = false)
            ok
        },
        onShare = { },
        onExport = {
            val ok = vm.saveResultToPublicAlbum(detail.imageUrl)
            toastSave(ok, export = true)
            ok
        },
        onDeleteFromGallery = if (fromGallery && !isFromPrivateSpace) {
            { vm.moveGalleryItemToRecycleBin(detail.id) }
        } else {
            null
        },
        isFromPrivateSpace = isFromPrivateSpace,
        onTogglePrivate = if (fromGallery) {
            {
                if (isFromPrivateSpace) {
                    onRequestRestoreFromPrivate(detail.id)
                } else {
                    onRequestMoveToPrivate(detail.id)
                }
            }
        } else {
            null
        },
    )
}

private sealed class PrivateAction {
    data class MoveToPrivate(val itemId: String) : PrivateAction()
    data object OpenPrivateSpace : PrivateAction()
}
