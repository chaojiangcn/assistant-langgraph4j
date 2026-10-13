# assistant-langgraph

[中文](README.zh-CN.md)

A Spring Boot assistant sample. It calls DeepSeek through Spring AI and runs a small LangGraph4j workflow that streams each step.

This is a small runnable backend that demonstrates two things:

- Call the DeepSeek chat model over HTTP, either as a single response or as a stream.
- Chain a `greeter` node and a `responder` node with LangGraph4j, then push each node over SSE.

The graph is `START → greeter → responder → END`. The greeter appends a greeting, and the responder appends an acknowledgement when it sees that greeting.

## Stack

| Component | Version |
| --- | --- |
| Java | 17 |
| Spring Boot | 4.0.8 |
| Spring AI | 2.0.1 |
| LangGraph4j | 1.9.2 |

Spring AI 2.x requires Spring Boot 4.x. The project also depends on `spring-boot-starter`, because Boot 4's `spring-boot-starter-web` no longer brings SnakeYAML. Without it, `application.yaml` is ignored.

## Run

The committed `src/main/resources/application.yaml` only contains the placeholder `api-key: ${DEEPSEEK_API_KEY:}`. It does not contain a real key. Provide a key in either of these ways:

1. Copy the example and fill in your own key. This file is listed in `.gitignore` and will not be committed.

```bash
copy config\application.yaml.example config\application.yaml
```

2. Set the `DEEPSEEK_API_KEY` environment variable. An environment variable overrides `config/application.yaml`. Start the app from a new terminal or run configuration; an already open terminal does not pick up the new value.

Current PowerShell window only:

```powershell
$env:DEEPSEEK_API_KEY = "your-api-key"
mvn spring-boot:run
```

Current cmd window only:

```bat
set DEEPSEEK_API_KEY=your-api-key
mvn spring-boot:run
```

Save it for the current Windows user. This does not update the current window, so open a new terminal afterwards:

```powershell
setx DEEPSEEK_API_KEY "your-api-key"
```

Current Linux / macOS terminal only:

```bash
export DEEPSEEK_API_KEY=your-api-key
mvn spring-boot:run
```

In IntelliJ IDEA, open Run/Debug Configurations and add `DEEPSEEK_API_KEY=your-api-key` under Environment variables.

Spring Boot lets `config/application.yaml` in the project root override the committed config. Configure either the local file or the environment variable, then start the app:

```bash
mvn spring-boot:run
```

The default port is `8080`.

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| GET | `/ai/generate?message=` | Call DeepSeek and return one response |
| GET | `/ai/generateStream?message=` | Call DeepSeek and stream the response |
| GET | `/ai/graph/run?message=` | Run the workflow as `text/event-stream`, one event per node |

Workflow example:

```bash
curl -N "http://localhost:8080/ai/graph/run?message=Let's,%20begin!"
```

Each event contains the node name and a snapshot of the messages at the end of that step:

```text
data:{"node":"greeter","messages":["Let's, begin!","Hello from GreeterNode!"]}
```

When `message` is omitted, the chat endpoints default to `Tell me a joke`, and the workflow defaults to `Let's, begin!`.
