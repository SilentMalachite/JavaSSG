# Getting Started with JavaSSG

This tutorial will guide you through creating your first static site with JavaSSG.

## Prerequisites

- Java 21 or higher
- Maven 3.9 or higher
- Basic knowledge of Markdown and HTML

## Step 1: Installation

### Option A: Build from Source

```bash
git clone https://github.com/yourusername/JavaSSG.git
cd JavaSSG
mvn clean package
```

### Option B: Create Native Application

```bash
mvn clean package jpackage:jpackage
```

## Step 2: Create Your First Site

### Generate Site Structure

```bash
# Using JAR
java -jar target/javassg-1.0.0.jar new site my-blog

# Using native app
./target/jpackage/JavaSSG/bin/JavaSSG new site my-blog
```

This creates the following structure:

```
my-blog/
├── config.yaml
├── content/
│   └── index.md
├── templates/
│   ├── base.html
│   └── page.html
├── static/
│   ├── css/
│   │   └── style.css
│   ├── js/
│   └── images/
└── _site/ (generated)
```

## Step 3: Configure Your Site

Edit `config.yaml`:

```yaml
site:
  title: "My Awesome Blog"
  description: "A blog about technology and life"
  url: "https://myblog.com"
  language: "en-US"
  author:
    name: "Your Name"
    email: "your@email.com"

build:
  contentDirectory: "content"
  outputDirectory: "_site"
  staticDirectory: "static"
  templatesDirectory: "templates"

server:
  port: 8080
  liveReload: true

blog:
  postsPerPage: 10
  generateArchive: true
  generateCategories: true
  generateTags: true
```

## Step 4: Create Content

### Create a Blog Post

```bash
java -jar target/javassg-1.0.0.jar new post "My First Post" --working-directory ./my-blog
```

This creates `content/posts/my-first-post.md`:

```markdown
---
title: "My First Post"
date: 2024-09-08
published: true
tags: [blog, first-post]
category: "General"
---

# My First Post

Welcome to my blog! This is my first post using JavaSSG.

## Features I Love

- Fast builds
- Live reload
- Security features
- Plugin system
```

### Create a Page

```bash
java -jar target/javassg-1.0.0.jar new page "About" --working-directory ./my-blog
```

## Step 5: Customize Templates

### Base Template (`templates/base.html`)

```html
<!DOCTYPE html>
<html lang="{{ site.language }}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{% if page.title %}{{ page.title }} - {% endif %}{{ site.title }}</title>
    <meta name="description" content="{{ page.description | default: site.description }}">
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
    <header>
        <h1><a href="/">{{ site.title }}</a></h1>
        <nav>
            <a href="/">Home</a>
            <a href="/about">About</a>
            <a href="/archive">Archive</a>
        </nav>
    </header>
    
    <main>
        {{ content }}
    </main>
    
    <footer>
        <p>&copy; 2024 {{ site.author.name }}</p>
    </footer>
</body>
</html>
```

### Post Template (`templates/post.html`)

```html
---
layout: base
---

<article>
    <header>
        <h1>{{ page.title }}</h1>
        <div class="meta">
            <time datetime="{{ page.date | date: 'yyyy-MM-dd' }}">
                {{ page.date | date: 'MMMM dd, yyyy' }}
            </time>
            {% if page.category %}
                <span class="category">{{ page.category }}</span>
            {% endif %}
            {% if page.tags %}
                <div class="tags">
                    {% for tag in page.tags %}
                        <span class="tag">{{ tag }}</span>
                    {% endfor %}
                </div>
            {% endif %}
        </div>
    </header>
    
    <div class="content">
        {{ content }}
    </div>
</article>
```

## Step 6: Start Development Server

```bash
java -jar target/javassg-1.0.0.jar serve --working-directory ./my-blog
```

Visit `http://localhost:8080` to see your site!

## Step 7: Build for Production

```bash
java -jar target/javassg-1.0.0.jar build --production --working-directory ./my-blog
```

The generated site will be in the `_site` directory.

## Next Steps

- Explore [Plugin System](../plugins.md)
- Learn about [Advanced Templates](../templates.md)
- Check out [Security Features](../security.md)
- Read [Performance Tips](../performance.md)