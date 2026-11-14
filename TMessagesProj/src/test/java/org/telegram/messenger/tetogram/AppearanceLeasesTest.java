package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceLeasesTest {
 @Test public void closingAnExportDoesNotReleaseAnEditor(){AppearanceLeases l=new AppearanceLeases();l.acquire("u1/media/a.jpg");l.acquire("u1/media/a.jpg");l.release("u1/media/a.jpg");assertTrue(l.pending("u1/media/a.jpg"));l.release("u1/media/a.jpg");assertFalse(l.pending("u1/media/a.jpg"));l.release("u1/media/a.jpg");assertFalse(l.pending("u1/media/a.jpg"));}
 @Test public void accountsAndFilesAreIndependent(){AppearanceLeases l=new AppearanceLeases();l.acquire("u1/media/a.jpg");l.acquire("u2/media/a.jpg");l.release("u1/media/a.jpg");assertTrue(l.pending("u2/media/a.jpg"));assertFalse(l.pending("u1/media/a.jpg"));assertFalse(l.pending("u2/media/b.jpg"));}
}
