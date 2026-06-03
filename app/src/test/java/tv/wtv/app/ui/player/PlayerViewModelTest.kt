package tv.wtv.app.ui.player

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.wtv.app.data.model.StreamInfo

class PlayerViewModelTest {

    private fun streamInfo() = StreamInfo(
        channelId = "ch-id",
        streamId = "stream-id",
        playbackUrl = "https://example.com/stream.m3u8",
        title = "Test Stream",
        startedAtMs = 1_767_225_600_000L,
        channelName = "TestChannel",
    )

    private fun playingState(
        qualities: List<String> = listOf("1080p60", "720p60", "480p30"),
        selected: String = "1080p60",
        isChatVisible: Boolean = true,
        isQualityPickerVisible: Boolean = false,
        isMetadataVisible: Boolean = true,
    ) = PlayerUiState.Playing(
        streamInfo = streamInfo(),
        availableQualities = qualities,
        selectedQuality = selected,
        isChatVisible = isChatVisible,
        isQualityPickerVisible = isQualityPickerVisible,
        isMetadataVisible = isMetadataVisible,
        chatMessages = emptyList(),
    )

    private fun viewModelWith(state: PlayerUiState): PlayerViewModel {
        val vm = PlayerViewModel()
        val field = vm.javaClass.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<PlayerUiState>).value = state
        return vm
    }

    @Test
    fun `initial state is Loading`() {
        assertEquals(PlayerUiState.Loading, PlayerViewModel().uiState.value)
    }

    @Test
    fun `selectQuality updates selected quality and closes picker`() = runTest {
        val vm = viewModelWith(playingState(selected = "1080p60", isQualityPickerVisible = true))
        vm.selectQuality("720p60")
        val state = vm.uiState.value as PlayerUiState.Playing
        assertEquals("720p60", state.selectedQuality)
        assertFalse(state.isQualityPickerVisible)
    }

    @Test
    fun `selectQuality is a no-op when not in Playing state`() = runTest {
        val vm = PlayerViewModel()
        vm.selectQuality("720p60")
        assertEquals(PlayerUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `toggleChat flips isChatVisible`() = runTest {
        val vm = viewModelWith(playingState(isChatVisible = true))
        vm.toggleChat()
        assertFalse((vm.uiState.value as PlayerUiState.Playing).isChatVisible)
        vm.toggleChat()
        assertTrue((vm.uiState.value as PlayerUiState.Playing).isChatVisible)
    }

    @Test
    fun `toggleQualityPicker flips isQualityPickerVisible`() = runTest {
        val vm = viewModelWith(playingState(isQualityPickerVisible = false))
        vm.toggleQualityPicker()
        assertTrue((vm.uiState.value as PlayerUiState.Playing).isQualityPickerVisible)
        vm.toggleQualityPicker()
        assertFalse((vm.uiState.value as PlayerUiState.Playing).isQualityPickerVisible)
    }

    @Test
    fun `toggleQualityPicker is a no-op when not in Playing state`() = runTest {
        val vm = PlayerViewModel()
        vm.toggleQualityPicker()
        assertEquals(PlayerUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `setAvailableQualities retains existing selected quality when still present`() = runTest {
        val vm = viewModelWith(playingState(qualities = listOf("Auto", "1080p60"), selected = "1080p60"))
        vm.setAvailableQualities(listOf("Auto", "1080p60", "720p60"))
        assertEquals("1080p60", (vm.uiState.value as PlayerUiState.Playing).selectedQuality)
    }

    @Test
    fun `setAvailableQualities falls back to first quality when selected is gone`() = runTest {
        val vm = viewModelWith(playingState(qualities = listOf("Auto", "1080p60"), selected = "1080p60"))
        vm.setAvailableQualities(listOf("Auto", "720p60"))
        assertEquals("Auto", (vm.uiState.value as PlayerUiState.Playing).selectedQuality)
    }
}
