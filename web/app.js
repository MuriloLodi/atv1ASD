const carrinho = new Map();
let produtos = [];

const moeda = valor => valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

async function carregarProdutos() {
    const resposta = await fetch('/api/produtos');
    produtos = await resposta.json();
    const lista = document.getElementById('lista-produtos');
    lista.innerHTML = '';

    produtos.forEach(produto => {
        const card = document.createElement('div');
        card.className = 'produto';
        card.innerHTML = `
            <h3>${produto.nome}</h3>
            <span>${produto.descricao}</span>
            <span class="preco">${moeda(produto.preco)}</span>
            <span class="estoque">Estoque: ${produto.estoque}</span>
        `;
        const botao = document.createElement('button');
        botao.textContent = 'Adicionar ao carrinho';
        botao.disabled = produto.estoque === 0;
        botao.onclick = () => adicionar(produto.id);
        card.appendChild(botao);
        lista.appendChild(card);
    });
}

function adicionar(id) {
    const produto = produtos.find(p => p.id === id);
    const atual = carrinho.get(id) || 0;
    if (atual >= produto.estoque) {
        alert('Quantidade maxima em estoque atingida');
        return;
    }
    carrinho.set(id, atual + 1);
    mostrarCarrinho();
}

function remover(id) {
    const atual = carrinho.get(id);
    if (atual > 1) {
        carrinho.set(id, atual - 1);
    } else {
        carrinho.delete(id);
    }
    mostrarCarrinho();
}

function mostrarCarrinho() {
    const lista = document.getElementById('itens-carrinho');
    lista.innerHTML = '';
    let total = 0;

    carrinho.forEach((quantidade, id) => {
        const produto = produtos.find(p => p.id === id);
        total += produto.preco * quantidade;
        const li = document.createElement('li');
        li.innerHTML = `<span>${quantidade}x ${produto.nome}</span><span>${moeda(produto.preco * quantidade)}</span>`;
        const botao = document.createElement('button');
        botao.textContent = '-';
        botao.onclick = () => remover(id);
        li.appendChild(botao);
        lista.appendChild(li);
    });

    if (carrinho.size === 0) {
        lista.innerHTML = '<li>Carrinho vazio</li>';
    }
    document.getElementById('total-carrinho').textContent = moeda(total);
}

async function buscarCep() {
    const cep = document.getElementById('cep').value.replace(/\D/g, '');
    const status = document.getElementById('cep-status');
    if (cep.length !== 8) {
        status.textContent = '';
        return;
    }
    status.textContent = 'Buscando endereco...';

    const resposta = await fetch(`/api/cep/${cep}`);
    const dados = await resposta.json();
    if (!resposta.ok) {
        status.textContent = dados.erro;
        return;
    }
    document.getElementById('logradouro').value = dados.logradouro;
    document.getElementById('bairro').value = dados.bairro;
    document.getElementById('cidade').value = dados.cidade;
    document.getElementById('uf').value = dados.uf;
    status.textContent = 'Endereco preenchido automaticamente';
}

async function comprar(evento) {
    evento.preventDefault();
    if (carrinho.size === 0) {
        alert('Adicione produtos ao carrinho');
        return;
    }

    const form = new FormData(evento.target);
    const pedido = {
        cliente: {
            nome: form.get('nome'),
            email: form.get('email'),
            cpf: form.get('cpf')
        },
        endereco: {
            cep: form.get('cep'),
            logradouro: form.get('logradouro'),
            numero: form.get('numero'),
            complemento: form.get('complemento'),
            bairro: form.get('bairro'),
            cidade: form.get('cidade'),
            uf: form.get('uf').toUpperCase()
        },
        pagamento: {
            numeroCartao: form.get('numeroCartao'),
            titular: form.get('titular'),
            validade: form.get('validade'),
            cvv: form.get('cvv')
        },
        itens: [...carrinho].map(([id, quantidade]) => ({ id, quantidade }))
    };

    const botao = document.getElementById('btn-comprar');
    botao.disabled = true;
    botao.textContent = 'Processando...';

    try {
        const resposta = await fetch('/api/pedidos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pedido)
        });
        const resultado = await resposta.json();
        mostrarResultado(resposta.ok, resultado);

        if (resposta.ok && resultado.sucesso) {
            carrinho.clear();
            mostrarCarrinho();
        }
        await carregarProdutos();
        await carregarEmails();
    } finally {
        botao.disabled = false;
        botao.textContent = '5. Confirmar compra';
    }
}

function mostrarResultado(ok, resultado) {
    const secao = document.getElementById('resultado');
    const mensagem = document.getElementById('mensagem-resultado');
    const etapas = document.getElementById('etapas');
    secao.classList.remove('oculto');
    etapas.innerHTML = '';

    if (!ok) {
        mensagem.className = 'erro';
        mensagem.textContent = resultado.erro;
        return;
    }

    mensagem.className = resultado.sucesso ? 'sucesso' : 'erro';
    mensagem.textContent = `${resultado.pedido} - ${resultado.mensagem} (total ${moeda(resultado.total)})`;

    resultado.etapas.forEach(etapa => {
        const li = document.createElement('li');
        li.value = etapa.numero;
        li.className = etapa.ok ? 'ok' : 'falha';
        li.textContent = etapa.descricao;
        etapas.appendChild(li);
    });

    if (resultado.entrega) {
        const li = document.createElement('li');
        li.style.listStyle = 'none';
        li.innerHTML = `<strong>Rastreio:</strong> ${resultado.entrega.codigoRastreio} - previsao ${resultado.entrega.previsao}`;
        etapas.appendChild(li);
    }
    secao.scrollIntoView({ behavior: 'smooth' });
}

async function carregarEmails() {
    const resposta = await fetch('/api/emails');
    const emails = await resposta.json();
    const lista = document.getElementById('lista-emails');
    lista.innerHTML = '';

    if (emails.length === 0) {
        lista.textContent = 'Nenhum e-mail enviado ainda.';
        return;
    }

    emails.slice().reverse().forEach(email => {
        const div = document.createElement('div');
        div.className = 'email';
        div.innerHTML = `
            <strong>${email.assunto}</strong><br>
            <small>Para: ${email.para} - ${email.enviadoEm}</small>
            <pre></pre>
        `;
        div.querySelector('pre').textContent = email.mensagem;
        lista.appendChild(div);
    });
}

document.getElementById('cep').addEventListener('blur', buscarCep);
document.getElementById('form-compra').addEventListener('submit', comprar);
document.getElementById('btn-emails').addEventListener('click', carregarEmails);

carregarProdutos();
mostrarCarrinho();
carregarEmails();
