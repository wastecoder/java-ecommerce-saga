# 🎬 Demo do fluxo (GIF)

Esta pasta contém o script que gera o **GIF do fluxo da SAGA** exibido no
[`README.md`](../../README.md) da raiz.

O GIF é produzido de forma **determinística** com o [vhs](https://github.com/charmbracelet/vhs)
(charmbracelet) a partir de [`flow.tape`](flow.tape): o vhs digita os comandos do *walkthrough* num
shell e grava a sessão. Por isso a stack precisa estar **rodando de verdade** durante a gravação — o
que aparece no GIF é a saída real da saga.

## Pré-requisitos

- **vhs** no PATH — `go install github.com/charmbracelet/vhs@latest` ou `brew install vhs`
  (veja a [doc do vhs](https://github.com/charmbracelet/vhs#installation)). O vhs usa `ttyd` e `ffmpeg`.
- **curl** e **jq** no PATH (o `flow.tape` declara `Require curl` / `Require jq`).
- A stack do ShopFlow de pé (veja "Como rodar" no README da raiz):

  ```bash
  docker compose up -d                  # infra
  ./gradlew :discovery-server:bootRun   # + order / inventory / payment / notification / api-gateway
  ```

## Gerar o GIF

Rode **a partir desta pasta** para que a saída caia em `doc/demo/flow.gif` (o `flow.tape` usa
`Output flow.gif`, relativo ao diretório de execução):

```bash
cd doc/demo
vhs flow.tape          # produz doc/demo/flow.gif
```

## Publicar no README

Depois de gerar `doc/demo/flow.gif`, **descomente** a linha do GIF na seção *"O fluxo na prática"* do
[`README.md`](../../README.md):

```markdown
![Demonstração do fluxo ShopFlow](doc/demo/flow.gif)
```

## Ajustes

- Tema/tamanho/velocidade: blocos `Set ...` no topo do [`flow.tape`](flow.tape).
- Os `Sleep` entre os comandos dão tempo para a saga assíncrona concluir antes de cada `GET`; se a sua
  máquina for mais lenta, aumente-os.
- Para gravar outros desfechos (cancelamento/rejeição), troque o corpo do pedido conforme descrito no
  README da raiz (total ≥ 1000 → `CANCELLED`; produto sem estoque → `REJECTED`).
