package ru.hh.school.unittesting.homework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

    @Mock
    private NotificationService notificationServiceMock;

    @Mock
    private UserService userServiceMock;

    private LibraryManager libraryManager;

    @BeforeEach
    void setUp() {
        libraryManager = new LibraryManager(notificationServiceMock, userServiceMock);
    }

    @Test
    void addBookShouldIncreaseInventoryWhenBookAdded() {
        libraryManager.addBook("book", 10);

        int howMuchBooksInInventory = libraryManager.getAvailableCopies("book");

        assertEquals(10, howMuchBooksInInventory);
    }

    @Test
    void borrowBookShouldSucceedWhenUserIsActiveAndBookIsAvailable() {
        when(userServiceMock.isUserActive("user")).thenReturn(true);

        libraryManager.addBook("book", 2);

        boolean result = libraryManager.borrowBook("book", "user");

        assertTrue(result);
        assertEquals(1, libraryManager.getAvailableCopies("book"));
        verify(notificationServiceMock).notifyUser("user", "You have borrowed the book: book");
    }

    @Test
    void borrowBookShouldFailWhenUserIsNotActive() {
        when(userServiceMock.isUserActive("user")).thenReturn(false);

        libraryManager.addBook("book", 5);

        boolean result = libraryManager.borrowBook("book", "user");

        assertFalse(result);
        assertEquals(5, libraryManager.getAvailableCopies("book"));
        verify(notificationServiceMock).notifyUser("user", "Your account is not active.");
    }

    @Test
    void borrowBookShouldFailWhenBookIsNotAvailable() {
        when(userServiceMock.isUserActive("user")).thenReturn(true);

        libraryManager.addBook("book", 0);

        boolean result = libraryManager.borrowBook("book", "user");

        assertFalse(result);
        assertEquals(0, libraryManager.getAvailableCopies("book"));
    }

    @Test
    void returnBookShouldSucceedWhenBookWasBorrowedByUser() {
        when(userServiceMock.isUserActive("user")).thenReturn(true);

        libraryManager.addBook("book", 3);
        libraryManager.borrowBook("book", "user");

        assertEquals(2, libraryManager.getAvailableCopies("book"));

        boolean result = libraryManager.returnBook("book", "user");

        assertTrue(result);
        assertEquals(3, libraryManager.getAvailableCopies("book"));
        verify(notificationServiceMock).notifyUser("user", "You have returned the book: book");
    }

    @Test
    void returnBookShouldFailWhenBookWasNotBorrowedByUser() {
        when(userServiceMock.isUserActive("user")).thenReturn(true);

        libraryManager.addBook("book", 3);
        libraryManager.addBook("book2", 2);
        libraryManager.borrowBook("book", "user");

        boolean result = libraryManager.returnBook("book2", "user");
        assertFalse(result);
    }

    @Test
    void returnBookShouldFailWhenBookWasNotBorrowed() {
        libraryManager.addBook("book", 3);

        boolean result = libraryManager.returnBook("book", "user");

        assertFalse(result);
    }

    @Test
    void returnBookShouldFailWhenBookWasBorrowedByAnotherUser() {
        when(userServiceMock.isUserActive("user2")).thenReturn(true);

        libraryManager.addBook("book1", 3);
        libraryManager.borrowBook("book1", "user2");

        boolean result = libraryManager.returnBook("book1", "user1");

        assertFalse(result);
    }

    @ParameterizedTest
    @CsvSource({
            "0, false, false, 0",
            "1, false, true, 0.4",
            "3, true, false, 2.25",
            "7, false, false, 3.5",
            "10, true, false, 7.5",
            "14, false, true, 5.6",
            "20, true, false, 15",
            "30, true, true, 18",
            "50, false, true, 20",
            "100, true, false, 75",
            "150, false, true, 60"
    })
    void testCalculateDynamicFeeLate(int overdueDays, boolean isBestseller, boolean isPremiumMember, double expectedLateFee) {
        assertEquals(expectedLateFee, libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember));
    }

    @Test
    void calculateDynamicFeeLateShouldThrowExceptionIfNegativeOverdueDays() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> libraryManager.calculateDynamicLateFee(-77, true, true));
        assertEquals("Overdue days cannot be negative.", exception.getMessage());
    }
}
