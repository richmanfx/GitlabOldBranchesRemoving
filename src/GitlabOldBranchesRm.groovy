/******************************************************************************
*                                                                             *
*      Удаляет старые смерженные ветки, начиная с даты "срока давности".      *
*      Если дата "срока давности" не была передана, то считаем ее равной      *
*      дате, которая была месяц тому назад.                                   *
*                                                                             *
******************************************************************************/

import groovy.time.TimeCategory

// Главна ветка - в неё переходим перед удалением
def mainBranch = "develop"

// Никогда не удалять эти ветки
final BRANCHES_TO_KEEP_ANYWAY = ['master', 'develop', 'HEAD']

if (args.size() < 1) {
    println "Запускать так: GitlabOldBranchesRm " +
            "путь_к_локальной_директории_проекта " +
            "[крайняя_дата_устаревания_ветки - dd-mm-yyyy]"
    return
}


// Параметр: директория репозитория
def repositoryDir = new File(args[0])


// Параметр: дата "срока давности" в формате dd-mm-yyyy.
// Ветки будут удалены если в них не было никакой активности после этой даты.
// Если дата "срока давности" не была передана,
// считаем ее равной дате, которая была месяц тому назад
def removeBeforeDate = new Date()
use(TimeCategory) {
    removeBeforeDate = removeBeforeDate - 1.month
}
if (args.size() > 1) {
    try {
        removeBeforeDate = Date.parse('dd-MM-yyyy', args[1])
    } catch (Exception ex) {
        println("Exception: " + ex)
    }
}

println "Срок давности веток: $removeBeforeDate"


/***   Замыкания. Начало блока.  ***/

// Выполнение команды консоли
// shellCommand - строка, содержащая команду
// workingDir - рабочая директория
// Возвращаемое значение: консольный вывод команды
def executeShellCommand = { shellCommand, workingDir ->
    def shellCommandProc = shellCommand.execute(null, workingDir)

    def out = new StringBuilder()
    def err = new StringBuilder()
    shellCommandProc.waitForProcessOutput(out, err)

    if (err) {
        // вывод команды просто выводим в консоль для информации
        System.err.print err.toString()
    }

    out.toString()

}


// Выполнение команды Git
// gitCommand - строка, содержащая команду
// Возвращаемое значение: консольный вывод команды
def executeGitCommand = { gitCommand ->
    executeShellCommand(gitCommand, repositoryDir)
}


// Получить дату последнего коммита в указанной ветке
// branchName - имя ветки
// Возвращаемое значение: дата последнего коммита
def getLastCommitDate = { branchName ->
    def lastCommitDate = executeGitCommand("git log $branchName -1 --pretty=format:%ci")
    Date.parse('yyyy-M-d H:m:s Z', lastCommitDate)
}


/***   Замыкания. Конец блока.  ***/

// Проверить существует ли вообще репозиторий Git по переданному пути
if (!repositoryDir.exists() || executeGitCommand('git rev-parse --git-dir').trim() != '.git') {
    System.err.println "Folder ${repositoryDir} is not valid Git repository"
    return
}

// Скрипт выполнять из "главной ветки"
println("Перейти на ветку '$mainBranch'")
print executeGitCommand("git checkout $mainBranch")

// Обновить список удаленных веток
println("Выполнить обновление списка удалённых веток: 'git fetch'")
print executeGitCommand('git fetch')
println("Удалить все ветки, которых нет во внешнем репозитории: 'git remote prune origin'")
print executeGitCommand('git remote prune origin')

// Удалить локальные ветки
def mergedBranchesOutput = executeGitCommand("git branch --merged $mainBranch")

mergedBranchesOutput.eachLine { branchLine ->
    // Определение имени локальной ветки
    def matcher = branchLine =~ /^\s*\*?\s*([^\s]*)$/
    if (!matcher) {
        return
    }
    def branch = matcher[0][1]

    if (// Если ветка отсутствует в списке неудаляемых
        !BRANCHES_TO_KEEP_ANYWAY.contains(branch) &&
            // в ветке не было активности после указанной даты
            getLastCommitDate(branch) < removeBeforeDate) {
                println "Удаляется локальная ветка: $branch"
                print executeGitCommand("git branch -d $branch")
    }
}

// Удалить ветки в дальнем репозитории с подтверждением

def remoteMergedBranchesOutput = executeGitCommand("git branch -r --merged $mainBranch")
def remoteBranchesToRemove = []

remoteMergedBranchesOutput.eachLine { remoteBranchLine ->
    // Определение имени ветки удаленного репозитория
    def matcher = remoteBranchLine =~ '^\\s*origin/([^\\s]*)$'
    if (!matcher) {
        return
    }
    def branch = matcher[0][1]
    def remoteBranch = "origin/$branch"

    if (// Если ветка отсутствует в списке неудаляемых
    !BRANCHES_TO_KEEP_ANYWAY.contains(branch) &&
            // в ветке не было активности после указанной даты
            getLastCommitDate(remoteBranch) < removeBeforeDate) {

        remoteBranchesToRemove.add(branch)
        println "Будет удалена дальняя ветка: $branch"
    }
}


// Веток для удаления нет
if (!remoteBranchesToRemove) {
    println 'Нет дальних веток для удаления'
    return
}


// Удалить дальнюю ветку с подтверждением
if (System.console().readLine('Вы уверены, что хотите удалить эти ветки (y/n)?').charAt(0).toString() == 'y') {
    remoteBranchesToRemove.each({ branch ->
        println "Удаляется дальняя ветка: $branch"
        print executeGitCommand("git push origin :$branch")
    })
}
