package demo.reversi.persistence;

import demo.reversi.model.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UserStore {
    private static final int INITIAL_CAPACITY = 8;
    private static final int MIN_CAPACITY = 4;
    private static final int RECORD_SIZE = 8192;
    private static final int HEADER_SIZE = Integer.BYTES * 3;
    private static final int RECORD_HEADER_SIZE = 1 + Integer.BYTES;

    private static final byte EMPTY = 0;
    private static final byte ACTIVE = 1;
    private static final byte DELETED = 2;

    private final Path dataFile;

    public UserStore() {
        Path folder = Path.of(System.getProperty("user.home"), ".javafx-reversi");
        this.dataFile = folder.resolve("users.dat");
        try {
            Files.createDirectories(folder);
            initializeFile();
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize user storage", e);
        }
    }

    private void initializeFile() throws IOException {
        if (!Files.exists(dataFile) || Files.size(dataFile) < HEADER_SIZE) {
            createEmptyFile(INITIAL_CAPACITY);
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "rw")) {
            if (!hasValidFileStructure(raf)) {
                createEmptyFile(INITIAL_CAPACITY);
            }
        }
    }

    private boolean hasValidFileStructure(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        int capacity = raf.readInt();
        int recordSize = raf.readInt();
        int userCount = raf.readInt();

        if (capacity < MIN_CAPACITY || recordSize != RECORD_SIZE || userCount < 0 || userCount > capacity) {
            return false;
        }

        long expectedLength = fileLengthFor(capacity);
        if (raf.length() != expectedLength) {
            return false;
        }

        int activeRecords = 0;
        for (int i = 0; i < capacity; i++) {
            raf.seek(positionOf(i));
            byte marker = raf.readByte();
            int length = raf.readInt();

            if (marker == ACTIVE) {
                if (length <= 0 || length > RECORD_SIZE - RECORD_HEADER_SIZE) {
                    return false;
                }
                activeRecords++;
            } else if (marker == EMPTY || marker == DELETED) {
                if (length != 0) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return activeRecords == userCount;
    }

    private void createEmptyFile(int capacity) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "rw")) {
            raf.setLength(0);
            writeHeader(raf, capacity, 0);
            raf.setLength(fileLengthFor(capacity));
        }
    }

    public User findByEmail(String email) throws IOException {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank()) {
            return null;
        }

        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "r")) {
            Header header = readHeader(raf);
            int index = findIndex(raf, header.capacity, normalized);
            return index == -1 ? null : readActiveSlot(raf, index);
        }
    }

    public void save(User user) throws IOException {
        Objects.requireNonNull(user, "user cannot be null");
        String normalized = normalizeEmail(user.getEmail());
        if (normalized.isBlank()) {
            throw new IOException("User email cannot be empty.");
        }

        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "rw")) {
            Header header = readHeader(raf);
            int existingIndex = findIndex(raf, header.capacity, normalized);

            if (existingIndex != -1) {
                writeSlot(raf, existingIndex, user);
                return;
            }

            int newCount = header.userCount + 1;
            if (newCount > header.capacity) {
                rehash(raf, capacityForUserCount(newCount));
                header = readHeader(raf);
            }

            int insertionIndex = findInsertionIndex(raf, header.capacity, normalized);
            if (insertionIndex == -1) {
                rehash(raf, capacityForUserCount(newCount));
                header = readHeader(raf);
                insertionIndex = findInsertionIndex(raf, header.capacity, normalized);
            }

            if (insertionIndex == -1) {
                throw new IOException("No empty slot found for the new user.");
            }

            writeSlot(raf, insertionIndex, user);
            writeUserCount(raf, newCount);
        }
    }

    public void delete(String email) throws IOException {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank()) {
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "rw")) {
            Header header = readHeader(raf);
            int index = findIndex(raf, header.capacity, normalized);
            if (index == -1) {
                return;
            }

            markDeleted(raf, index);
            int newCount = header.userCount - 1;
            writeUserCount(raf, newCount);

            if (newCount == 0) {
                rehash(raf, INITIAL_CAPACITY);
            } else if (newCount <= header.capacity / 4) {
                rehash(raf, capacityForUserCount(newCount));
            }
        }
    }

    public int getUserCount() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "r")) {
            return readHeader(raf).userCount;
        }
    }

    public int getCapacity() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "r")) {
            return readHeader(raf).capacity;
        }
    }

    private int capacityForUserCount(int userCount) {
        if (userCount <= 0) {
            return INITIAL_CAPACITY;
        }
        return Math.max(MIN_CAPACITY, userCount * 2);
    }

    private List<User> loadAllUsers(RandomAccessFile raf, int capacity) throws IOException {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            if (readMarker(raf, i) == ACTIVE) {
                users.add(readActiveSlot(raf, i));
            }
        }
        return users;
    }

    private void rehash(RandomAccessFile raf, int requestedCapacity) throws IOException {
        Header oldHeader = readHeader(raf);
        List<User> users = loadAllUsers(raf, oldHeader.capacity);
        int newCapacity = Math.max(MIN_CAPACITY, requestedCapacity);
        if (newCapacity < users.size()) {
            newCapacity = capacityForUserCount(users.size());
        }
        recreateFileWithUsers(raf, users, newCapacity);
    }

    private void recreateFileWithUsers(RandomAccessFile raf, List<User> users, int newCapacity) throws IOException {
        raf.setLength(0);
        writeHeader(raf, newCapacity, users.size());
        raf.setLength(fileLengthFor(newCapacity));

        for (User user : users) {
            int index = findInsertionIndex(raf, newCapacity, user.getEmail());
            if (index == -1) {
                throw new IOException("Rehash failed: no slot found.");
            }
            writeSlot(raf, index, user);
        }
    }

    private int findIndex(RandomAccessFile raf, int capacity, String normalizedEmail) throws IOException {
        int start = indexFor(normalizedEmail, capacity);
        for (int step = 0; step < capacity; step++) {
            int index = (start + step) % capacity;
            byte marker = readMarker(raf, index);

            if (marker == EMPTY) {
                return -1;
            }
            if (marker == DELETED) {
                continue;
            }

            User user = readActiveSlot(raf, index);
            if (user != null && normalizeEmail(user.getEmail()).equals(normalizedEmail)) {
                return index;
            }
        }
        return -1;
    }

    private int findInsertionIndex(RandomAccessFile raf, int capacity, String email) throws IOException {
        String normalizedEmail = normalizeEmail(email);
        int start = indexFor(normalizedEmail, capacity);
        int firstDeleted = -1;

        for (int step = 0; step < capacity; step++) {
            int index = (start + step) % capacity;
            byte marker = readMarker(raf, index);

            if (marker == EMPTY) {
                return firstDeleted != -1 ? firstDeleted : index;
            }
            if (marker == DELETED && firstDeleted == -1) {
                firstDeleted = index;
            }
        }
        return firstDeleted;
    }

    private User readActiveSlot(RandomAccessFile raf, int index) throws IOException {
        raf.seek(positionOf(index));
        byte marker = raf.readByte();
        if (marker != ACTIVE) {
            return null;
        }

        int length = raf.readInt();
        if (length <= 0 || length > RECORD_SIZE - RECORD_HEADER_SIZE) {
            throw new IOException("Invalid record length at index " + index + ".");
        }

        byte[] data = new byte[length];
        raf.readFully(data);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (User) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not deserialize user at index " + index + ".", e);
        }
    }

    private void writeSlot(RandomAccessFile raf, int index, User user) throws IOException {
        byte[] data = serialize(user);
        if (data.length > RECORD_SIZE - RECORD_HEADER_SIZE) {
            throw new IOException("Serialized user record is too large.");
        }

        raf.seek(positionOf(index));
        raf.writeByte(ACTIVE);
        raf.writeInt(data.length);
        raf.write(data);

        int remainingBytes = RECORD_SIZE - RECORD_HEADER_SIZE - data.length;
        if (remainingBytes > 0) {
            raf.write(new byte[remainingBytes]);
        }
    }

    private byte[] serialize(User user) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(user);
            oos.flush();
            return baos.toByteArray();
        }
    }

    private void markDeleted(RandomAccessFile raf, int index) throws IOException {
        raf.seek(positionOf(index));
        raf.writeByte(DELETED);
        raf.writeInt(0);
        int remainingBytes = RECORD_SIZE - RECORD_HEADER_SIZE;
        if (remainingBytes > 0) {
            raf.write(new byte[remainingBytes]);
        }
    }

    private byte readMarker(RandomAccessFile raf, int index) throws IOException {
        raf.seek(positionOf(index));
        return raf.readByte();
    }

    private Header readHeader(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        int capacity = raf.readInt();
        int recordSize = raf.readInt();
        int userCount = raf.readInt();
        if (capacity < MIN_CAPACITY || recordSize != RECORD_SIZE || userCount < 0 || userCount > capacity) {
            throw new IOException("Invalid user-store header.");
        }
        return new Header(capacity, userCount);
    }

    private void writeHeader(RandomAccessFile raf, int capacity, int userCount) throws IOException {
        raf.seek(0);
        raf.writeInt(capacity);
        raf.writeInt(RECORD_SIZE);
        raf.writeInt(userCount);
    }

    private void writeUserCount(RandomAccessFile raf, int userCount) throws IOException {
        raf.seek(Integer.BYTES * 2L);
        raf.writeInt(userCount);
    }

    private int indexFor(String email, int capacity) {
        return Math.floorMod(normalizeEmail(email).hashCode(), capacity);
    }

    private long positionOf(int index) {
        return HEADER_SIZE + (long) index * RECORD_SIZE;
    }

    private long fileLengthFor(int capacity) {
        return HEADER_SIZE + (long) capacity * RECORD_SIZE;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record Header(int capacity, int userCount) { }
}