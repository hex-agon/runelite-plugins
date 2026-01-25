package work.fking.nexnostalgia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NexClipPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexClipPlayer.class);

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);

    private final ExecutorService executorService;

    public NexClipPlayer(int concurrentSounds) {
        executorService = new ThreadPoolExecutor(
                concurrentSounds,
                concurrentSounds,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> new Thread(runnable, "NexClipPlayer-" + THREAD_COUNTER.getAndIncrement())
        );
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public void play(VoiceOver voiceOver, float volume) {
        executorService.execute(() -> loadAndPlay(voiceOver, volume));
    }

    private void loadAndPlay(VoiceOver voiceOver, float volume) {
        LOGGER.debug("Loading and playing voiceOver={}", voiceOver);
        try (Clip audioClip = AudioSystem.getClip()) {
            var stream = getClass().getResourceAsStream("/sounds/" + voiceOver.file());

            if (stream == null) {
                LOGGER.warn("Missing audio file {}", voiceOver.file());
                return;
            }

            try (var fileStream = new BufferedInputStream(stream);
                    var audioStream = AudioSystem.getAudioInputStream(fileStream)) {
                audioClip.open(audioStream);
            }
            var countDownLatch = new CountDownLatch(1);

            var control = (FloatControl) audioClip.getControl(Type.MASTER_GAIN);
            control.setValue(volume);
            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    countDownLatch.countDown();
                }
            });
            audioClip.start();

            // wait for the clip to finish playing
            if (!countDownLatch.await(15, TimeUnit.SECONDS)) {
                LOGGER.debug("Timed out waiting for audio clip to end");
            }
        } catch (LineUnavailableException | UnsupportedAudioFileException | InterruptedException | IOException e) {
            LOGGER.warn("Failed to play voice over: {}", e.getMessage());
        }
    }
}
