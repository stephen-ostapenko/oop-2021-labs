package ru.kotlin.senin

val testRequestData = RequestData("username", "password", "test-owner", "test-repository")

data class TestCommitInfo(val sha: String, val commit: CommitDetails, val author: UserInfo?, val files: List<Change>, val timeFromStart: Long)
data class TestCommitWithChanges(val timeFromStart: Long, val commit: CommitWithChanges)

const val commitsRequestDelay = 1000L

val user1ShortInfo = AuthorInfo("Vasiliy Pupkin", "2020-12-08T21:30:48Z")
val user1Info = UserInfo("pupkin", 1, "User")

val user2ShortInfo = AuthorInfo("Ivanov Ivan", "2019-12-08T21:30:48Z")
val user2Info = UserInfo("ivanov", 2, "User")

val user3ShortInfo = AuthorInfo("github-classroom[bot]", "2020-11-08T21:30:48Z")
val user3Info = UserInfo("bot3", 3, "Bot")

val user4ShortInfo = AuthorInfo("Deleted user", "2016-11-08T21:30:48Z")

fun committedByUser1(message: String) = CommitDetails(user1ShortInfo, message)
fun committedByUser2(message: String) = CommitDetails(user2ShortInfo, message)
fun committedByUser3(message: String) = CommitDetails(user3ShortInfo, message)
fun committedByUser4(message: String) = CommitDetails(user4ShortInfo, message)

const val file1 = "a.txt"
const val file2 = "b.txt"
const val file3 = "c.txt"

val changeFile1_1 = Change("1", file1, 5, 5, 10)   // user1   - 10
val changeFile2_1 = Change("2", file2, 10, 0, 10)  // user1   - 10
val changeFile3_1 = Change("3", file3, 5, 0, 5)    // user2 - 5
val changeFile1_2 = Change("4", file1, 0, 5, 5)    // bot - ignore
val changeFile2_2 = Change("5", file2, 5, 5, 10)   // deleted user - ignore
val changeFile3_2 = Change("6", file3, 10, 0, 10)  // user1   - 10
val changeFile1_3 = Change("7", file1, 10, 5, 15)  // user1   - 15
val changeFile2_3 = Change("8", file2, 10, 10, 20) // user2 - 20
val changeFile2_4 = Change("9", file2, 0, 10, 10)  // user2 - 10

val testCommits = listOf(
    TestCommitInfo("1", committedByUser1("Commit 1"), user1Info, listOf(changeFile1_1, changeFile2_1),400L),
    TestCommitInfo("2", committedByUser2("Commit 2"), user2Info, listOf(changeFile3_1),600L),
    TestCommitInfo("3", committedByUser3("Commit by Bot"), user3Info, listOf(changeFile1_2), 800L),
    TestCommitInfo("4", committedByUser4("Old Commit"), null, listOf(changeFile2_2), 1000L),
    TestCommitInfo("5", committedByUser1("Commit 5"), user1Info, listOf(changeFile3_2, changeFile1_3),1200L),
    TestCommitInfo("6", committedByUser2("Commit 6"), user2Info, listOf(changeFile2_3, changeFile2_4),1400L)
)

val testCommitsWithChanges = testCommits.map {
    TestCommitWithChanges(it.timeFromStart, CommitWithChanges(it.sha, it.author, it.files))
}

val commits = testCommits.map {
    Commit(it.sha, it.commit, it.author)
}

data class TestResults(val login2Stat: Map<String, UserStatistics>)

val progressResults = listOf(
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(1, sortedSetOf(file1, file2), 20)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(1, sortedSetOf(file1, file2), 20),
            user2Info.login to UserStatistics(1, sortedSetOf(file3), 5)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(2, sortedSetOf(file1, file2, file3), 45),
            user2Info.login to UserStatistics(1, sortedSetOf(file3), 5)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(2, sortedSetOf(file1, file2, file3), 45),
            user2Info.login to UserStatistics(2, sortedSetOf(file3, file2), 35)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(3, sortedSetOf(file1, file2, file3), 65),
            user2Info.login to UserStatistics(2, sortedSetOf(file3, file2), 35)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(3, sortedSetOf(file1, file2, file3), 65),
            user2Info.login to UserStatistics(3, sortedSetOf(file3, file2), 40)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(4, sortedSetOf(file1, file2, file3), 90),
            user2Info.login to UserStatistics(3, sortedSetOf(file3, file2), 40)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(4, sortedSetOf(file1, file2, file3), 90),
            user2Info.login to UserStatistics(4, sortedSetOf(file3, file2), 70)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(5, sortedSetOf(file1, file2, file3), 110),
            user2Info.login to UserStatistics(4, sortedSetOf(file3, file2), 70)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(5, sortedSetOf(file1, file2, file3), 110),
            user2Info.login to UserStatistics(5, sortedSetOf(file3, file2), 75)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(6, sortedSetOf(file1, file2, file3), 135),
            user2Info.login to UserStatistics(5, sortedSetOf(file3, file2), 75)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(6, sortedSetOf(file1, file2, file3), 135),
            user2Info.login to UserStatistics(6, sortedSetOf(file3, file2), 105)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(7, sortedSetOf(file1, file2, file3), 155),
            user2Info.login to UserStatistics(6, sortedSetOf(file3, file2), 105)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(7, sortedSetOf(file1, file2, file3), 155),
            user2Info.login to UserStatistics(7, sortedSetOf(file3, file2), 110)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(8, sortedSetOf(file1, file2, file3), 180),
            user2Info.login to UserStatistics(7, sortedSetOf(file3, file2), 110)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(8, sortedSetOf(file1, file2, file3), 180),
            user2Info.login to UserStatistics(8, sortedSetOf(file3, file2), 140)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(9, sortedSetOf(file1, file2, file3), 200),
            user2Info.login to UserStatistics(8, sortedSetOf(file3, file2), 140)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(9, sortedSetOf(file1, file2, file3), 200),
            user2Info.login to UserStatistics(9, sortedSetOf(file3, file2), 145)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(10, sortedSetOf(file1, file2, file3), 225),
            user2Info.login to UserStatistics(9, sortedSetOf(file3, file2), 145)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(10, sortedSetOf(file1, file2, file3), 225),
            user2Info.login to UserStatistics(10, sortedSetOf(file3, file2), 175)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(11, sortedSetOf(file1, file2, file3), 245),
            user2Info.login to UserStatistics(10, sortedSetOf(file3, file2), 175)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(11, sortedSetOf(file1, file2, file3), 245),
            user2Info.login to UserStatistics(11, sortedSetOf(file3, file2), 180)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(12, sortedSetOf(file1, file2, file3), 270),
            user2Info.login to UserStatistics(11, sortedSetOf(file3, file2), 180)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(12, sortedSetOf(file1, file2, file3), 270),
            user2Info.login to UserStatistics(12, sortedSetOf(file3, file2), 210)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(13, sortedSetOf(file1, file2, file3), 290),
            user2Info.login to UserStatistics(12, sortedSetOf(file3, file2), 210)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(13, sortedSetOf(file1, file2, file3), 290),
            user2Info.login to UserStatistics(13, sortedSetOf(file3, file2), 215)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(14, sortedSetOf(file1, file2, file3), 315),
            user2Info.login to UserStatistics(13, sortedSetOf(file3, file2), 215)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(14, sortedSetOf(file1, file2, file3), 315),
            user2Info.login to UserStatistics(14, sortedSetOf(file3, file2), 245)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(15, sortedSetOf(file1, file2, file3), 335),
            user2Info.login to UserStatistics(14, sortedSetOf(file3, file2), 245)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(15, sortedSetOf(file1, file2, file3), 335),
            user2Info.login to UserStatistics(15, sortedSetOf(file3, file2), 250)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(16, sortedSetOf(file1, file2, file3), 360),
            user2Info.login to UserStatistics(15, sortedSetOf(file3, file2), 250)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(16, sortedSetOf(file1, file2, file3), 360),
            user2Info.login to UserStatistics(16, sortedSetOf(file3, file2), 280)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(17, sortedSetOf(file1, file2, file3), 380),
            user2Info.login to UserStatistics(16, sortedSetOf(file3, file2), 280)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(17, sortedSetOf(file1, file2, file3), 380),
            user2Info.login to UserStatistics(17, sortedSetOf(file3, file2), 285)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(18, sortedSetOf(file1, file2, file3), 405),
            user2Info.login to UserStatistics(17, sortedSetOf(file3, file2), 285)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(18, sortedSetOf(file1, file2, file3), 405),
            user2Info.login to UserStatistics(18, sortedSetOf(file3, file2), 315)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(19, sortedSetOf(file1, file2, file3), 425),
            user2Info.login to UserStatistics(18, sortedSetOf(file3, file2), 315)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(19, sortedSetOf(file1, file2, file3), 425),
            user2Info.login to UserStatistics(19, sortedSetOf(file3, file2), 320)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(20, sortedSetOf(file1, file2, file3), 450),
            user2Info.login to UserStatistics(19, sortedSetOf(file3, file2), 320)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(20, sortedSetOf(file1, file2, file3), 450),
            user2Info.login to UserStatistics(20, sortedSetOf(file3, file2), 350)
        )
    ),
    TestResults(
        mapOf(
            user1Info.login to UserStatistics(20, sortedSetOf(file1, file2, file3), 450),
            user2Info.login to UserStatistics(20, sortedSetOf(file3, file2), 350)
        )
    )
)