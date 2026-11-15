package org.telegram.messenger.tetogram;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceParameterNamesTest {
 @Test public void russianSearchTermsExistForTechnicalRoles(){String label=AppearanceParameterNames.label("dark.chat_outBubbleSelected",true);assertTrue(label.contains("Исходящие"));assertTrue(label.contains("Пузырь"));assertTrue(label.contains("Выбранное"));}
 @Test public void englishLabelsAreReadableAndKeysAreNotMutated(){String key="list.chats_unreadCounterMuted";assertEquals("List · Chats · Unread · Counter · Muted",AppearanceParameterNames.label(key,false));assertEquals("list.chats_unreadCounterMuted",key);}
}
