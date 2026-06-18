package work.fking.nexnostalgia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SoundPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoundPlayer.class);

    private static final AudioFormat OUTPUT_FORMAT = new AudioFormat(44100, 16, 2, true, false);

    private static final int MIN_VOLUME_PERCENT = 0;
    private static final int MAX_VOLUME_PERCENT = 125;

    private static final int BUFFER_FRAMES = 1024;
    private static final int BUFFER_SIZE = BUFFER_FRAMES * OUTPUT_FORMAT.getFrameSize();

    private final BlockingQueue<VoiceOver> pendingQueue;
    private final BlockingQueue<PlayingVoiceOver> readyQueue;

    private final AtomicInteger masterVolume = new AtomicInteger(100);
    private volatile boolean shutdown = false;

    private final Thread loaderThread;
    private final Thread mixerThread;
    private final SourceDataLine line;

    public SoundPlayer(int queueCapacity) throws LineUnavailableException {
        this.pendingQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.readyQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.line = openLine();

        this.loaderThread = new Thread(this::loaderLoop, "NexNostalgia-SoundPlayer-Loader");
        this.loaderThread.setDaemon(true);
        this.loaderThread.start();

        this.mixerThread = new Thread(this::mixerLoop, "NexNostalgia-SoundPlayer-Mixer");
        this.mixerThread.setDaemon(true);
        this.mixerThread.start();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void shutdown() {
        shutdown = true;
        loaderThread.interrupt();
        mixerThread.interrupt();
    }

    public void setMasterVolume(int volume) {
        masterVolume.set(Math.max(MIN_VOLUME_PERCENT, Math.min(MAX_VOLUME_PERCENT, volume)));
    }

    public void play(VoiceOver voiceOver) {
        if (shutdown) {
            return;
        }
        if (!pendingQueue.offer(voiceOver)) {
            LOGGER.debug("Sound queue full, dropping voiceOver={}", voiceOver);
        }
    }

    private void mixerLoop() {
        List<PlayingVoiceOver> playingVoiceOvers = new ArrayList<>();

        try (line) {
            line.start();

            var mixBuffer = new int[BUFFER_FRAMES * OUTPUT_FORMAT.getChannels()];
            var outBuffer = new byte[BUFFER_SIZE];

            while (!shutdown) {
                drainReadyQueueTo(playingVoiceOvers);

                if (playingVoiceOvers.isEmpty()) {
                    try {
                        playingVoiceOvers.add(readyQueue.take());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                Arrays.fill(mixBuffer, 0);

                var masterGain = masterVolume.get() / 100.0f;
                var it = playingVoiceOvers.iterator();

                while (it.hasNext()) {
                    var sound = it.next();
                    if (sound.mixInto(mixBuffer, masterGain)) {
                        sound.close();
                        it.remove();
                    }
                }

                for (int i = 0; i < BUFFER_FRAMES; i++) {
                    var sampleLeft = clamp(mixBuffer[i * 2]);
                    var sampleRight = clamp(mixBuffer[i * 2 + 1]);

                    outBuffer[i * 4] = (byte) (sampleLeft & 0xFF);
                    outBuffer[i * 4 + 1] = (byte) ((sampleLeft >> 8) & 0xFF);
                    outBuffer[i * 4 + 2] = (byte) (sampleRight & 0xFF);
                    outBuffer[i * 4 + 3] = (byte) ((sampleRight >> 8) & 0xFF);
                }
                line.write(outBuffer, 0, BUFFER_SIZE);
            }

            line.drain();
            line.stop();
        } finally {
            clearReadyQueue();
            playingVoiceOvers.forEach(PlayingVoiceOver::close);
        }
    }

    private void drainReadyQueueTo(List<PlayingVoiceOver> playingVoiceOvers) {
        PlayingVoiceOver sound;
        while ((sound = readyQueue.poll()) != null) {
            playingVoiceOvers.add(sound);
        }
    }

    private void clearReadyQueue() {
        PlayingVoiceOver queued;
        while ((queued = readyQueue.poll()) != null) {
            queued.close();
        }
    }

    private void loaderLoop() {
        while (!shutdown) {
            var pendingVoiceOver = takeNextVoiceOver();

            if (pendingVoiceOver == null) {
                continue;
            }
            var stream = getClass().getResourceAsStream("/sounds/" + pendingVoiceOver.file());

            if (stream == null) {
                continue;
            }
            var pcmStream = toPcmStream(stream);

            if (pcmStream == null) {
                closeQuietly(stream);
                continue;
            }

            if (!readyQueue.offer(new PlayingVoiceOver(pcmStream))) {
                LOGGER.debug("Failed to queue voice over, readyQueue is full");
                closeQuietly(pcmStream);
                closeQuietly(stream);
            }
        }
    }

    private VoiceOver takeNextVoiceOver() {
        try {
            return pendingQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private AudioInputStream toPcmStream(InputStream stream) {
        AudioInputStream rawStream = null;
        try {
            rawStream = AudioSystem.getAudioInputStream(stream);

            return AudioSystem.getAudioInputStream(OUTPUT_FORMAT, rawStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            LOGGER.warn("Failed to decode audio stream: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.info("Failed to convert audio stream to specified format: {}", e.getMessage());
        }

        if (rawStream != null) {
            closeQuietly(rawStream);
        }
        return null;
    }

    private void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private static SourceDataLine openLine() throws LineUnavailableException {
        var line = AudioSystem.getSourceDataLine(OUTPUT_FORMAT);
        line.open(OUTPUT_FORMAT, BUFFER_SIZE * 2);
        return line;
    }

    private static int clamp(int value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    private static final class PlayingVoiceOver {

        private final AudioInputStream pcmStream;
        private final byte[] readBuf;

        PlayingVoiceOver(AudioInputStream pcmStream) {
            this.pcmStream = pcmStream;
            this.readBuf = new byte[BUFFER_SIZE];
        }

        boolean mixInto(int[] mixBuffer, float masterGain) {
            var bytesNeeded = SoundPlayer.BUFFER_FRAMES * OUTPUT_FORMAT.getFrameSize();
            int bytesRead = 0;
            try {
                while (bytesRead < bytesNeeded) {
                    var n = pcmStream.read(readBuf, bytesRead, bytesNeeded - bytesRead);
                    if (n == -1) {
                        break;
                    }
                    bytesRead += n;
                }
            } catch (IOException e) {
                LOGGER.warn("Error reading audio stream: {}", e.getMessage());
                return true;
            }
            var samplesRead = bytesRead / 2;

            for (int i = 0; i < samplesRead; i++) {
                var lo = readBuf[i * 2] & 0xFF;
                var hi = readBuf[i * 2 + 1] & 0xFF;
                var sample = (short) (lo | (hi << 8));
                mixBuffer[i] += (int) (sample * masterGain);
            }
            return bytesRead < bytesNeeded;
        }

        void close() {
            try {
                pcmStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
