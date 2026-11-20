package fr.shikkanime.jobs

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.impl.StdSchedulerFactory
import org.quartz.impl.matchers.GroupMatcher
import kotlin.test.Test
import kotlin.test.assertEquals

@Expression("0 0 12 ? * *")
private class ScheduledJob : Job {
    override fun execute(context: JobExecutionContext) = Unit
}

@Expression("0 30 18 ? * *")
private class OtherScheduledJob : Job {
    override fun execute(context: JobExecutionContext) = Unit
}

private class UnscheduledJob : Job {
    override fun execute(context: JobExecutionContext) = Unit
}

@DisplayName("tests for the scheduler job registration")
class SchedulerTest {

    @Nested
    @DisplayName("tests for the 'schedule' extension")
    inner class ScheduleTests {
        @Test
        @DisplayName("should register only the jobs carrying an expression")
        fun `should register only the jobs carrying an expression`() {
            // Given
            val scheduler = StdSchedulerFactory().scheduler

            try {
                // When
                val scheduled = scheduler.schedule(
                    listOf(ScheduledJob(), OtherScheduledJob(), UnscheduledJob())
                )

                // Then: the two expressed jobs are registered, the bare one is skipped
                assertEquals(2, scheduled)
                assertEquals(2, scheduler.getJobKeys(GroupMatcher.anyGroup()).size)
            } finally {
                scheduler.shutdown()
            }
        }
    }
}
