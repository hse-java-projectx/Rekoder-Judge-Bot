# Rekoder Judge Bot

A bot that automatically updates the content of the service

## Installation

First, make sure `git` and `maven` are installed on your machine. Then execute following commands

```bash
$ git clone https://github.com/hse-java-projectx/Rekoder-Judge-Bot
$ mvn compile
```

## Execution

```bash
$ cd target/classes
$ java rekoder.bot.RekoderBot
RJB>
```

## Usage

To get actual help, type `help` into cli

| Command        | Arguments           | Explanation  |
| ------------- |:-------------:| -----:|
| `update`      | `<judge-name>` | Fetch recent problems from judge |
| `exit`      |      |   Exit from CLI |
| `help` | | Get CLI help      |
| `list` | | Get list of all active judge interactors      |
| `tasks` | | Get queued tasks count      |

## 