package org.telegram.messenger.tetogram;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChatTranslationRulesTest {
    @Test public void sameLanguageIgnoresRegion(){assertTrue(ChatTranslationRules.sameLanguage("pt-BR","pt"));assertTrue(ChatTranslationRules.sameLanguage("zh_TW","ZH"));assertFalse(ChatTranslationRules.sameLanguage("ru","uk"));assertFalse(ChatTranslationRules.sameLanguage(null,"ru"));assertFalse(ChatTranslationRules.sameLanguage("","ru"));}
    @Test public void winnerNeedsEnoughVotesAndMajority(){HashMap<String,Integer> v=new HashMap<>();assertNull(ChatTranslationRules.winner(null));v.put("de",2);assertNull(ChatTranslationRules.winner(v));v.put("de",3);assertEquals("de",ChatTranslationRules.winner(v));v.put("en",3);assertNull(ChatTranslationRules.winner(v));v.put("de",4);assertEquals("de",ChatTranslationRules.winner(v));}
    @Test public void sentKeyIgnoresTrimmedWhitespaceButNotChat(){assertEquals(ChatTranslationRules.sentKey(5,"Hallo  "),ChatTranslationRules.sentKey(5," Hallo"));assertNotEquals(ChatTranslationRules.sentKey(5,"Hallo"),ChatTranslationRules.sentKey(6,"Hallo"));assertNotEquals(ChatTranslationRules.sentKey(5,"Hallo"),ChatTranslationRules.sentKey(5,"Hello"));}
    @Test public void failedTranslationWaitsAMinute(){assertTrue(ChatTranslationRules.retry(null,0));assertFalse(ChatTranslationRules.retry(1000L,30_000));assertTrue(ChatTranslationRules.retry(1000L,61_000));}
}
