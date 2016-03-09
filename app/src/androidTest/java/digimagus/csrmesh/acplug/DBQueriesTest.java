package digimagus.csrmesh.acplug;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.IsolatedContext;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

/**
 */
public class DBQueriesTest extends AndroidTestCase {
    private static final String TEST_FILE_PREFIX = "test_";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockContentResolver resolver = new MockContentResolver();
        RenamingDelegatingContext targetContextWrapper = new RenamingDelegatingContext(
                new MockContext(), // The context that most methods are delegated to
                getContext(), // The context that file methods are delegated to
                TEST_FILE_PREFIX);
        Context context = new IsolatedContext(resolver, targetContextWrapper);
        setContext(context);
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }


}
